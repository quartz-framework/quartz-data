package xyz.quartzframework.data;

import xyz.quartzframework.data.annotation.Storage;
import xyz.quartzframework.data.query.QuartzQuery;
import xyz.quartzframework.data.storage.InMemoryStorage;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Storage
public interface FakeStorage extends InMemoryStorage<FakeEntity, UUID> {

    List<FakeEntity> findByName(String name);

    List<FakeEntity> findByScoreGreaterThan(int minScore);

    List<FakeEntity> findByActiveTrue();

    List<FakeEntity> findByIdIn(Collection<UUID> ids);

    @QuartzQuery("find where name like ?1")
    List<FakeEntity> searchByName(String pattern);

    @QuartzQuery("find top 2 where score > ?1 order by score desc")
    List<FakeEntity> topScorers(int minScore);

    List<FakeEntity> findByNameAndActiveTrue(String name);

    List<FakeEntity> findByScoreLessThanAndActiveTrue(int maxScore);

    List<FakeEntity> findByCreatedAtAfter(Instant time);

    boolean existsByName(String name);

    long countByActiveTrue();

    List<FakeEntity> findByNameNotLike(String pattern);

    List<FakeEntity> findByNameIn(Collection<String> names);

    List<FakeEntity> findByNameIsNotNull();

    List<FakeEntity> findTop2ByActiveTrueOrderByScoreDesc();

    Optional<FakeEntity> findFirstByActiveTrueOrderByCreatedAtDesc();

    @QuartzQuery("find where score >= ?1 and active = true order by score desc")
    List<FakeEntity> findActivesWithMinScore(int score);

    @QuartzQuery("find where name not like ?1 order by createdAt asc")
    List<FakeEntity> findByNameExclusionPattern(String pattern);

    @QuartzQuery("find top 1 where score < ?1 and active = true order by createdAt desc")
    Optional<FakeEntity> findRecentLowScorer(int maxScore);

    @QuartzQuery("count where name like ?1")
    long countMatchingNames(String pattern);

    @QuartzQuery("exists where name = ?1 and active = true")
    boolean existsActiveByName(String name);

    @QuartzQuery("find where createdAt >= ?1 and createdAt <= ?2 order by createdAt desc")
    List<FakeEntity> findBetweenDates(Instant from, Instant to);

    @QuartzQuery("find where name in ?1 order by name desc")
    List<FakeEntity> findAllByNameInDesc(Collection<String> names);

    @QuartzQuery("exists where score >= ?1 and active = true")
    boolean existsByMinScoreAndActive(int score);

    @QuartzQuery("count where score >= ?1")
    long countByScoreGreaterThanEqual(int score);

    @QuartzQuery("count where active = true and score < ?1")
    long countActiveLowScorers(int maxScore);

    @QuartzQuery("exists where createdAt > ?1")
    boolean existsByCreatedAfter(Instant date);

    List<FakeEntity> findByNameIsNull();

}