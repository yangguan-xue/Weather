package com.ygx.weatherreport.repository;

import com.ygx.weatherreport.model.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long>{
    /**
     * 根据微信 openid 查找用户
     * 这将自动生成查询：SELECT * FROM users WHERE openid = ?
     */
    Optional<UserEntity> findByOpenid(String openid);

    /**
     * 检查指定的 openid 是否已存在
     */
    boolean existsByOpenid(String openid);
}
