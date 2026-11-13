package com.example.githubsearcher.repository;

import com.example.githubsearcher.entity.GitHubRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Data Access Object interface for GitHubRepository entities,
 * supporting standard CRUD operations and dynamic specifications.
 */
@Repository
public interface GitHubRepositoryDao extends JpaRepository<GitHubRepository, Long>, JpaSpecificationExecutor<GitHubRepository> {
}

