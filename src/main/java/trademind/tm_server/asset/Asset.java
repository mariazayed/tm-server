package trademind.tm_server.asset;

import jakarta.persistence.*;
import lombok.*;
import trademind.tm_server.base.BaseEntity;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.trade.Trade;

import java.util.List;


@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Table(name = AppConstants.DB_ASSETS)
public class Asset extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String symbol;

    @OneToMany(mappedBy = "asset", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Trade> trades;

}