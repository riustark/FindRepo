package com.example.githubsearcher.repository;

import com.example.githubsearcher.entity.GitHubRepository;
import com.example.githubsearcher.specification.RepositorySpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class GitHubRepositoryDaoIntegrationTest {

    @Autowired
    private GitHubRepositoryDao repositoryDao;

    @BeforeEach
    void setUp() {
        repositoryDao.deleteAll();
    }

    @Test
    void shouldPersistNewRepositories() {
        GitHubRepository repo = GitHubRepository.builder()
                .id(101L)
                .name("repo-one")
                .owner("user1")
                .language("Java")
                .stars(500)
                .forks(50)
                .htmlUrl("https://github.com/user1/repo-one")
                .build();

        repositoryDao.save(repo);

        assertThat(repositoryDao.findById(101L)).isPresent();
        assertThat(repositoryDao.findById(101L).get().getName()).isEqualTo("repo-one");
    }

    @Test
    void shouldUpdateExistingRepositoryRatherThanDuplicate() {
        GitHubRepository original = GitHubRepository.builder()
                .id(101L)
                .name("repo-one")
                .owner("user1")
                .language("Java")
                .stars(500)
                .forks(50)
                .htmlUrl("https://github.com/user1/repo-one")
                .build();
        repositoryDao.save(original);

        GitHubRepository updated = GitHubRepository.builder()
                .id(101L)
                .name("repo-one-renamed")
                .owner("user1")
                .language("Java")
                .stars(1200)
                .forks(90)
                .htmlUrl("https://github.com/user1/repo-one")
                .build();
        repositoryDao.save(updated);

        assertThat(repositoryDao.count()).isEqualTo(1);
        GitHubRepository retrieved = repositoryDao.findById(101L).orElseThrow();
        assertThat(retrieved.getName()).isEqualTo("repo-one-renamed");
        assertThat(retrieved.getStars()).isEqualTo(1200);
    }

    @Test
    void shouldHandleNullableDescriptionAndLanguage() {
        GitHubRepository repo = GitHubRepository.builder()
                .id(202L)
                .name("minimal-repo")
                .owner("user2")
                .description(null)
                .language(null)
                .stars(10)
                .forks(1)
                .htmlUrl("https://github.com/user2/minimal-repo")
                .build();

        repositoryDao.save(repo);

        GitHubRepository retrieved = repositoryDao.findById(202L).orElseThrow();
        assertThat(retrieved.getDescription()).isNull();
        assertThat(retrieved.getLanguage()).isNull();
    }

    @Test
    void shouldReturnEmptyListWhenDatabaseIsEmpty() {
        Page<GitHubRepository> page = repositoryDao.findAll(PageRequest.of(0, 10));
        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void shouldFilterByLanguage() {
        seedSampleRepositories();

        Specification<GitHubRepository> spec = RepositorySpecification.hasLanguage("Java");
        List<GitHubRepository> results = repositoryDao.findAll(spec);

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(r -> "Java".equalsIgnoreCase(r.getLanguage()));
    }

    @Test
    void shouldFilterByMinimumStars() {
        seedSampleRepositories();

        Specification<GitHubRepository> spec = RepositorySpecification.hasMinStars(1000);
        List<GitHubRepository> results = repositoryDao.findAll(spec);

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(r -> r.getStars() >= 1000);
    }

    @Test
    void shouldFilterByLanguageAndMinimumStarsTogether() {
        seedSampleRepositories();

        Specification<GitHubRepository> spec = Specification
                .where(RepositorySpecification.hasLanguage("Java"))
                .and(RepositorySpecification.hasMinStars(2000));

        List<GitHubRepository> results = repositoryDao.findAll(spec);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("java-popular");
    }

    @Test
    void shouldSortByStarsDescending() {
        seedSampleRepositories();

        Page<GitHubRepository> page = repositoryDao.findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "stars")));

        List<Integer> stars = page.getContent().stream().map(GitHubRepository::getStars).toList();
        assertThat(stars).isSortedAccordingTo((a, b) -> b.compareTo(a));
    }

    @Test
    void shouldSortByForksDescending() {
        seedSampleRepositories();

        Page<GitHubRepository> page = repositoryDao.findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "forks")));

        List<Integer> forks = page.getContent().stream().map(GitHubRepository::getForks).toList();
        assertThat(forks).isSortedAccordingTo((a, b) -> b.compareTo(a));
    }

    @Test
    void shouldSortByLastUpdatedDescending() {
        seedSampleRepositories();

        Page<GitHubRepository> page = repositoryDao.findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "lastUpdated")));

        List<OffsetDateTime> dates = page.getContent().stream().map(GitHubRepository::getLastUpdated).toList();
        assertThat(dates).isSortedAccordingTo((a, b) -> b.compareTo(a));
    }

    private void seedSampleRepositories() {
        GitHubRepository r1 = GitHubRepository.builder()
                .id(1L)
                .name("java-popular")
                .owner("corp")
                .language("Java")
                .stars(5000)
                .forks(500)
                .lastUpdated(OffsetDateTime.parse("2024-03-01T10:00:00Z"))
                .htmlUrl("https://github.com/corp/java-popular")
                .build();

        GitHubRepository r2 = GitHubRepository.builder()
                .id(2L)
                .name("java-modest")
                .owner("corp")
                .language("Java")
                .stars(300)
                .forks(50)
                .lastUpdated(OffsetDateTime.parse("2024-02-01T10:00:00Z"))
                .htmlUrl("https://github.com/corp/java-modest")
                .build();

        GitHubRepository r3 = GitHubRepository.builder()
                .id(3L)
                .name("python-popular")
                .owner("pythonista")
                .language("Python")
                .stars(4000)
                .forks(800)
                .lastUpdated(OffsetDateTime.parse("2024-04-01T10:00:00Z"))
                .htmlUrl("https://github.com/pythonista/python-popular")
                .build();

        repositoryDao.saveAll(List.of(r1, r2, r3));
    }
}

