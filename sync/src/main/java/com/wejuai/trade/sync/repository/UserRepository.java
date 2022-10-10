package com.wejuai.trade.sync.repository;

import com.wejuai.entity.mysql.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
}
