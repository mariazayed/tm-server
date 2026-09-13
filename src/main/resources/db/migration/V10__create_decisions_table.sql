-- New "Decisions Log" feature: records personal/strategic decisions
-- (Velucra AI, book projects, content, trading, investing, personal) so
-- overlapping and superseding decisions over time have a persistent record
-- instead of living only in a Google Doc. Shaped to match Decision.java
-- (BaseEntity fields + Decision's own), following the same column-naming
-- convention as goals/reminder_notes.

CREATE TABLE decisions (
    id                       bigserial    PRIMARY KEY,
    decision_number          integer,
    decision_date            timestamp    NOT NULL,
    title                    varchar(255) NOT NULL,
    category                 varchar(255) NOT NULL,
    status                   varchar(255) NOT NULL,
    description              text         NOT NULL,
    reasoning                text,
    related_decision_number  integer,
    amount                   double precision,
    created_at               timestamp    NOT NULL DEFAULT now(),
    updated_at               timestamp,
    deleted_at               timestamp,
    notes                    text
);

COMMENT ON COLUMN decisions.decision_number IS 'Sequence number from the original decisions log (1, 2, 3...), independent of id';
COMMENT ON COLUMN decisions.category IS 'DecisionCategoryEnum: VELUCRA_AI, BOOK_WRITING, CONTENT, TRADING, INVESTING, PERSONAL, SYSTEM';
COMMENT ON COLUMN decisions.status IS 'DecisionStatusEnum: ACTIVE, SUPERSEDED, REVERSED, COMPLETED';
COMMENT ON COLUMN decisions.related_decision_number IS 'decision_number of an earlier decision this one reconsiders/reverses, if any';
COMMENT ON COLUMN decisions.amount IS 'Optional $ figure tied to the decision (e.g. trading capital committed)';

-- Backfill: the 11 decisions from the "Decisions" log (26 Jun - 2 Sep 2026),
-- reconstructed in English from the source document (Google Docs blocks raw
-- text scraping, so these are summaries pieced together from a fetched
-- preview -- not verbatim quotes). Review and correct them once the page is
-- live, especially the original Arabic titles/wording if you want those
-- preserved verbatim.
INSERT INTO decisions (decision_number, decision_date, title, category, status, description, reasoning, related_decision_number, amount) VALUES
(1, '2026-06-26 00:00:00', 'Founded Velucra AI (then Busala AI)', 'VELUCRA_AI', 'SUPERSEDED',
 'Founding day of the company (originally named Busala AI, later renamed Velucra). Vision: build an advanced, semi-autonomous AI platform that gathers data, analyzes companies, monitors economic news, evaluates investment opportunities, assesses risk, and generates professional investment reports with minimal human intervention. Scoped an initial set of roughly 14 system tasks spanning company analysis, economic forecasting, and investment-opportunity assessment.',
 NULL, NULL, NULL),
(2, '2026-07-03 00:00:00', 'Decided to write a book on investment and economics', 'BOOK_WRITING', 'SUPERSEDED',
 'Committed to writing a systematic guide integrating scattered knowledge of economics, accounting, company analysis, and valuation into one structured methodology, developed in parallel with the Velucra AI platform.',
 'Ten personal motivations were listed, including building a deep, systematic understanding of the subject rather than fragmented knowledge, plus ten reasons for developing the book alongside Velucra AI rather than sequentially.',
 NULL, NULL),
(3, '2026-07-12 00:00:00', 'Decided to write a detailed company-analysis book', 'BOOK_WRITING', 'SUPERSEDED',
 'Committed to a second, more detailed reference book converting fragmented company-analysis expertise into a repeatable process, developed in parallel with Velucra AI.',
 'Seven stated purposes for writing it, emphasizing integrating knowledge and methodology with the platform''s technical implementation.',
 2, NULL),
(4, '2026-08-01 00:00:00', 'Launched an educational Instagram page ("The Wealth Mind")', 'CONTENT', 'SUPERSEDED',
 'Opened an educational Instagram account under the name "The Wealth Mind" to build a personal brand alongside the other projects.',
 'Ten listed benefits, including building a personal brand and consolidating knowledge, brand identity, and the platform under one identity.',
 NULL, NULL),
(5, '2026-08-06 00:00:00', 'Renamed Busala AI to Velucra; replaced "The Wealth Mind" with "Velucra Learn"', 'SYSTEM', 'SUPERSEDED',
 'Two-part decision: (1) renamed the company from Busala AI to Velucra, citing global professionalism and room for the company to expand; (2) discontinued The Wealth Mind Instagram page in favor of a new "Velucra Learn" page tied to the platform''s own brand.',
 NULL, 4, NULL),
(6, '2026-08-20 00:00:00', 'Full system restructuring', 'SYSTEM', 'SUPERSEDED',
 'A complete restructuring of priorities after recognizing that running Velucra AI, social media, trading, and investing simultaneously was creating psychological stress and diffusing focus. Proposed 8 structural changes, allocated a total of $1,250 to trading, and set a recovery timeline running through the end of February 2027.',
 'Psychological stress and lack of focus from running too many projects in parallel.',
 NULL, 1250),
(7, '2026-08-22 00:00:00', 'Reconsidered decision 6', 'SYSTEM', 'SUPERSEDED',
 'Revised decision 6 after further reflection. Kept its 8 core points but scaled Velucra AI back to an entertainment/learning-only project, dropped paid recommendation-group subscriptions, and capped initial trading capital at $500.',
 'Further reflection on decision 6''s scope and cost.',
 6, 500),
(8, '2026-08-23 00:00:00', 'Reconsidered decision 7', 'SYSTEM', 'SUPERSEDED',
 'Minor adjustment to decision 7: reduced the trading starting capital from an intended $1,000 down to $500 based on a fresh financial assessment.',
 'Financial assessment showed $500 was the more realistic starting point.',
 7, 500),
(9, '2026-08-26 00:00:00', 'Subscribed to a trading recommendation channel', 'TRADING', 'SUPERSEDED',
 'After beginning to implement the restructured system, subscribed to a recommendation channel despite having rejected that in decision 7, while committing to still respect the money-management plan already in place.',
 'Wanted the extra signal source but chose to keep it inside the existing risk/money-management plan.',
 8, NULL),
(10, '2026-09-01 00:00:00', 'Personal commitment to the capital management plan', 'TRADING', 'REVERSED',
 'A personal pledge to adhere strictly to the capital-management plan through 21 April 2027, including keeping daily trading logs and a psychological self-evaluation, with a reward tied to at least 90% compliance.',
 'Wanted external accountability for sticking to the risk plan long-term.',
 9, NULL),
(11, '2026-09-02 00:00:00', 'Stopped trading entirely; refocused on personal wellbeing and passive investing', 'PERSONAL', 'ACTIVE',
 'Same-day reversal of decision 10 after a rapid capital loss under the very risk-management protocol just committed to. Concluded that trading does not suit the author, and reset priorities to personal wellness, long-term passive index-fund investing, and only modest project work done for learning''s sake.',
 'Rapid capital loss on the first day of following the strict plan led to the conclusion that trading isn''t a good fit personally.',
 10, NULL);
