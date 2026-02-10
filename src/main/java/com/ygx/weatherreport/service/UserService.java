// service/UserService.java
package com.ygx.weatherreport.service;

import com.ygx.weatherreport.model.entity.UserEntity;
import com.ygx.weatherreport.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    /**
     * 核心方法：通过 openid 查找或创建用户。
     * 这是微信登录流程中最常用的方法。
     * 1. 先用 openid 查询用户是否存在。
     * 2. 如果存在，更新其最后登录时间等信息（可选）。
     * 3. 如果不存在，则创建一个新的用户记录。
     *
     * @param openid 微信用户的唯一标识
     * @param nickname 用户昵称（从微信获取）
     * @param avatarUrl 用户头像URL（从微信获取）
     * @param city 用户所在城市
     * @return 查询到或新创建的 UserEntity 对象
     */
    @Transactional
    public UserEntity findOrCreateByOpenid(String openid, String nickname, String avatarUrl, String city) {
        // 1. 尝试查找现有用户
        Optional<UserEntity> userOptional = userRepository.findByOpenid(openid);

        if (userOptional.isPresent()) {
            // 用户已存在，更新信息（例如最后登录时间、可能的头像昵称变更）
            UserEntity existingUser = userOptional.get();
            // 可以选择性地更新来自微信的最新信息
            existingUser.setNickname(nickname);
            existingUser.setAvatarUrl(avatarUrl);
            if (city != null && !city.isEmpty()) {
                existingUser.setCity(city);
            }
            // 注意：updatedAt 字段已由 @PreUpdate 自动处理
            return userRepository.save(existingUser);
        } else {
            // 2. 用户不存在，创建新用户
            UserEntity newUser = new UserEntity();
            newUser.setOpenid(openid);
            newUser.setNickname(nickname);
            newUser.setAvatarUrl(avatarUrl);
            newUser.setCity(city);
            // createdAt 和 updatedAt 会由 @PrePersist 自动设置
            return userRepository.save(newUser);
        }
    }

    /**
     * 根据 openid 查找用户（单纯查询，不创建）
     * @param openid 微信 openid
     * @return 用户实体，如果不存在则返回 null
     */
    public UserEntity findByOpenid(String openid) {
        return userRepository.findByOpenid(openid).orElse(null);
    }

    /**
     * 根据用户ID查找用户
     * @param id 用户表主键ID
     * @return 用户实体
     */
    public UserEntity findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    /**
     * 更新用户信息（例如更新城市、昵称等）
     * @param user 要更新的用户实体对象
     * @return 更新后的用户实体
     */
    public UserEntity updateUser(UserEntity user) {
        // 直接保存，@PreUpdate 会自动处理 updatedAt
        return userRepository.save(user);
    }

    /**
     * 检查 openid 是否已注册
     */
    public boolean existsByOpenid(String openid) {
        return userRepository.existsByOpenid(openid);
    }
}
