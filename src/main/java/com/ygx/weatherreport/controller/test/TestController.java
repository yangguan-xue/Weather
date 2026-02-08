package com.ygx.weatherreport.controller.test;

import com.ygx.weatherreport.repository.UserRepository;
import com.ygx.weatherreport.utils.ResponseWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 测试控制器，用于学习和测试
 *注解说明:
 * @RestController: 标识这是一个Controller，并且返回JSON数据
 * @RequestMapping("/test"): 这个Controller下所有接口都以/api/test开头
 */
@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private UserRepository userRepository;

    /**
     * GET请求示例
     * 访问: GET https://localhost:8080/api/test/hello
     * 作用：最基本的测试接口
     */
    @GetMapping("/hello")
    public ResponseWrapper<String> hello(){
        String message = "您好，知晴雨天气后端服务启动中！";
        return ResponseWrapper.success(message);
    }

    /**
     * 数据库状态测试
     */
    /*
    @GetMapping("/db-status")
    public ResponseWrapper<Map<String, Object>> testDatabase(){

        try{
            //测试数据库连接
            Long userCount = UserRepository.findAll();

            Map<String, Object> data = new HashMap<>();
            data.put("status","数据库连接正常");
            data.put("userCount", userCount);
            data.put("timestamp", LocalDateTime.now());

            return ResponseWrapper.success(data);
        } catch (Exception e) {
            return ResponseWrapper.error("数据库连接失败：" + e.getMessage());
        }
    }
    */

    // 最简单的Repository测试
    @GetMapping("/repo")
    public ResponseWrapper<String> testRepo() {
        if (userRepository == null) {
            return ResponseWrapper.error("userRepository 为 null！");
        }

        try {
            long count = userRepository.count();
            return ResponseWrapper.success("Repository测试成功！用户数：" + count);
        } catch (Exception e) {
            return ResponseWrapper.error("Repository调用失败：" + e.getMessage());
        }


    }
}
