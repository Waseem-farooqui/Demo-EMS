package com.was.employeemanagementsystem.repository;

import com.was.employeemanagementsystem.entity.Rota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RotaRepository extends JpaRepository<Rota, Long> {

    List<Rota> findByDepartmentOrderByUploadedDateDesc(String department);

    List<Rota> findByStartDateBetweenOrderByStartDateDesc(LocalDate start, LocalDate end);

    List<Rota> findAllByOrderByUploadedDateDesc();
}

