package com.securelab.experiment.repository;

import com.securelab.experiment.entity.ExperimentRun;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExperimentRunRepository extends JpaRepository<ExperimentRun, Long> {

}
