package com.securelab.experiment.repository;

import java.util.List;
import java.util.Optional;

import com.securelab.experiment.entity.AttackScenario;

import org.springframework.data.repository.Repository;

/**
 * 읽기 전용 리포지토리. 시나리오 추가·수정은 Flyway 마이그레이션으로만 한다.
 */
public interface AttackScenarioRepository extends Repository<AttackScenario, Long> {

	List<AttackScenario> findAllByOrderByIdAsc();

	Optional<AttackScenario> findByCode(String code);

}
