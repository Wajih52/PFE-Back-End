package tn.weeding.agenceevenementielle.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import tn.weeding.agenceevenementielle.entities.AnalyseAiHistorique;

import java.util.List;

public interface AnalyseAiHistoriqueRepository extends JpaRepository<AnalyseAiHistorique,Long> {

    List<AnalyseAiHistorique> findTop20ByOrderByCreatedAtDesc();
}
