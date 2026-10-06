package trademind.tm_server.invest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import trademind.tm_server.constants.AppConstants;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the security model. SecurityConfig protects exactly {@code /api/v1/invest/**}, so every
 * investing controller must live under that prefix; a controller added with a different base path
 * would silently be open. This test fails the build when that happens.
 */
class ProtectedRoutesInventoryTest {

    private static List<Class<?>> controllersIn(String basePackage) throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        List<Class<?>> found = new ArrayList<>();
        for (BeanDefinition bd : scanner.findCandidateComponents(basePackage)) {
            found.add(Class.forName(bd.getBeanClassName()));
        }
        return found;
    }

    private static String[] basePaths(Class<?> controller) {
        RequestMapping mapping = controller.getAnnotation(RequestMapping.class);
        return mapping == null ? new String[0] : mapping.value();
    }

    @Test
    void everyInvestControllerIsMountedUnderTheProtectedPrefix() throws Exception {
        List<Class<?>> controllers = controllersIn("trademind.tm_server.invest");
        assertFalse(controllers.isEmpty(), "the scan found no investing controllers; the test is broken");

        for (Class<?> controller : controllers) {
            String[] paths = basePaths(controller);
            assertTrue(paths.length > 0, controller.getSimpleName() + " has no class-level @RequestMapping");
            for (String path : paths) {
                assertTrue(path.equals(AppConstants.URL_INVEST) || path.startsWith(AppConstants.URL_INVEST + "/"),
                        controller.getSimpleName() + " is mounted at " + path
                                + ", outside " + AppConstants.URL_INVEST + " which is the only protected prefix");
            }
        }
    }

    @Test
    void theOnlyOpenControllerIsLoginAndItsMe() throws Exception {
        List<String> authPaths = new ArrayList<>();
        for (Class<?> controller : controllersIn("trademind.tm_server.auth")) {
            for (String path : basePaths(controller)) {
                authPaths.add(controller.getSimpleName() + " " + path);
            }
        }
        // AuthController owns /auth/login (public) and /auth/me (authenticated); InvestController lives
        // under /invest. Nothing else in the auth package may expose routes.
        assertEquals(List.of("AuthController " + AppConstants.URL_AUTH), authPaths.stream()
                .filter(p -> !p.startsWith("InvestController")).sorted().toList());
    }

    @Test
    void thePrefixConstantsHaveNotDrifted() {
        assertEquals("/api/v1/invest", AppConstants.URL_INVEST);
        assertEquals("/api/v1/auth", AppConstants.URL_AUTH);
    }
}
