package com.atguigu.spzx.manager.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.atguigu.spzx.common.exception.GuiguException;
import com.atguigu.spzx.common.log.annotation.Log;
import com.atguigu.spzx.manager.mapper.SysRoleUserMapper;
import com.atguigu.spzx.manager.mapper.SysUserMapper;
import com.atguigu.spzx.manager.service.SysUserService;
import com.atguigu.spzx.model.dto.system.AssginRoleDto;
import com.atguigu.spzx.model.dto.system.LoginDto;
import com.atguigu.spzx.model.dto.system.SysUserDto;
import com.atguigu.spzx.model.entity.system.SysUser;
import com.atguigu.spzx.model.vo.common.ResultCodeEnum;
import com.atguigu.spzx.model.vo.system.LoginVo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.core.util.Json;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class SysUserServiceImpl implements SysUserService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    @Autowired
    private SysRoleUserMapper sysRoleUserMapper ;

    @Override
    public LoginVo login(LoginDto loginDto) {
        // 校正验证码

        // 1 获取输入验证码和存储到redis的key名字
        String captcha = loginDto.getCaptcha();
        String key = loginDto.getCodeKey();

        // 2 根据获取的redis里面key，查询redis里面存储验证码
        String s = redisTemplate.opsForValue().get("user:validate"+key);

        // 3 比较验证码，如果不一致，提示用户
        if (StrUtil.isEmpty(s) || !StrUtil.equalsIgnoreCase(s,captcha)){
            throw new GuiguException(ResultCodeEnum.VALIDATECODE_ERROR);
        }

        // 4 删除redis中的验证码
        redisTemplate.delete("user:validate"+key);

        // 1 获取提交用户名，LoginDto获取到
        String userName = loginDto.getUserName();
        // 2 根据用户名查询数据库表sys_user表
        SysUser sysUser = sysUserMapper.selectUserInfoByUserName(userName);
        // 3 如果根据用户名查不到对应信息，用户不存在，返回错误信息
        if (sysUser == null){
            //throw new RuntimeException("User doesn't exist");
            throw new GuiguException(ResultCodeEnum.LOGIN_ERROR);
        }
        // 4 如果根据用户名查询到用户信息，用户存在
        // 5 获取输入的密码，比较输入的密码和数据库密码是否一致
        String db_pw = sysUser.getPassword();
        String input_pw = loginDto.getPassword();
        // db_pw is encryped, wouldn't match what is given by user directnly
        // we need to encrypt what user inputs first, use MD5 encryption
        input_pw = DigestUtils.md5DigestAsHex(input_pw.getBytes());

        if (!Objects.equals(db_pw, input_pw)){
            //throw new RuntimeException("Incorrect password!");
            throw new GuiguException(ResultCodeEnum.LOGIN_ERROR);
        }

        // 6 如果密码一致，登陆成功，如果密码不一致登陆失败
        // 7 登陆成功，生成用户唯一标识token
        // 生成token方法有很多种，此处用UUID生成
        String token = UUID.randomUUID().toString().replaceAll("-","");

        // 8 把登陆成功用户信息放到redis里面
        // key: token, value: user info
        redisTemplate.opsForValue().set(
                "user:login" + token,
                JSON.toJSONString(sysUser),
                7,
                TimeUnit.DAYS
        );
        // 9 返回Loginvo对象
        LoginVo loginVo = new LoginVo();
        loginVo.setToken(token);

        return loginVo;
    }

    @Override
    public SysUser getUserInfo(String token) {
        // 2 根据token，查询redis里面存储用户信息
        String userJson = redisTemplate.opsForValue().get("user:login" + token);

       // ObjectMapper objectMapper = new ObjectMapper();
        //SysUser sysUser = objectMapper.readValue(userJson, SysUser.class);
        return JSON.parseObject(userJson, SysUser.class);
    }

    @Override
    public void logout(String token) {
        redisTemplate.delete("user:login" + token);
    }

    @Override
    public PageInfo<SysUser> findByPage(SysUserDto sysUserDto, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum , pageSize);
        List<SysUser> sysUserList = sysUserMapper.findByPage(sysUserDto) ;
        return new PageInfo(sysUserList);
    }

    @Override
    public void saveSysUser(SysUser sysUser) {
        // 根据输入的用户名查询用户
        SysUser dbSysUser = sysUserMapper.findByUserName(sysUser.getUserName()) ;
        if(dbSysUser != null) {
            throw new GuiguException(ResultCodeEnum.USER_NAME_IS_EXISTS) ;
        }

        // 对密码进行加密
        String password = sysUser.getPassword();
        String digestPassword = DigestUtils.md5DigestAsHex(password.getBytes());
        sysUser.setPassword(digestPassword);
        sysUser.setStatus(1);
        sysUserMapper.saveSysUser(sysUser) ;
    }

    @Override
    public void updateSysUser(SysUser sysUser) {
        sysUserMapper.updateSysUser(sysUser) ;
    }

    @Override
    public void deleteById(Long userId) {
        sysUserMapper.deleteById(userId) ;
    }

    @Log(title = "JJJ模块" , businessType = 2 )
    @Transactional
    @Override
    public void doAssign(AssginRoleDto assginRoleDto) {

        // 删除之前的所有的用户所对应的角色数据
        sysRoleUserMapper.deleteByUserId(assginRoleDto.getUserId()) ;

        //TODO 为了测试，模拟异常
        //int a = 1/0;

        // 分配新的角色数据
        List<Long> roleIdList = assginRoleDto.getRoleIdList();
        roleIdList.forEach(roleId->{
            sysRoleUserMapper.doAssign(assginRoleDto.getUserId(),roleId);
        });
    }
}
