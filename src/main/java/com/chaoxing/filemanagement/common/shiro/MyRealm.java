package com.chaoxing.filemanagement.common.shiro;

import com.chaoxing.filemanagement.service.UserService;
import com.chaoxing.filemanagement.util.JWTUtil;
import com.chaoxing.filemanagement.vo.UserVO;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
public class MyRealm extends AuthorizingRealm {


    private UserService userService;

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    /**
     * 大坑！，必须重写此方法，不然Shiro会报错
     */
    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof JWTToken;
    }

    /**
     * 只有当需要检测用户权限的时候才会调用此方法，例如checkRole,checkPermission之类的    Authorization  授权
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        Integer userId = JWTUtil.getUserId(principals.toString());
        SimpleAuthorizationInfo simpleAuthorizationInfo = new SimpleAuthorizationInfo();
        UserVO user = userService.selectUserVO(userId);
        //simpleAuthorizationInfo.addRole(user.getPermission());
        System.out.println("permission:"+user.getPermission());
        Set<String> permission = new HashSet<>(Arrays.asList(user.getPermission().split(";")));
        simpleAuthorizationInfo.addStringPermissions(permission);
        return simpleAuthorizationInfo;
    }

    /**
     * 默认使用此方法进行用户名正确与否验证，错误抛出异常即可。 Authentication  认证方式
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken auth) throws AuthenticationException {
        String token = (String) auth.getCredentials();
        // 解密获得username，用于和数据库进行对比
        String username = JWTUtil.getUsername(token);
        Integer userId = JWTUtil.getUserId(token);

        System.out.println("userId"+userId);
        System.out.println("userName"+username);
        if (username == null) {
            throw new AuthenticationException("token invalid");
        }


        UserVO userBean = userService.selectUserVO(userId);
        if (userBean == null) {
            throw new AuthenticationException("User didn't existed!");
        }

        if (! JWTUtil.verify(token,userBean.getPassword())) {
            throw new AuthenticationException("Username or password error");
        }


        return new SimpleAuthenticationInfo(token, token, "my_realm");
    }
}
