package com.zhong.xspring.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zhong.xspring.annotation.XAutowired;
import com.zhong.xspring.annotation.XController;
import com.zhong.xspring.annotation.XRequestMapping;
import com.zhong.xspring.service.TestService;

/**
 * @author zhong
 * @date 2019年8月29日 下午12:49:26
 * 
 */
@XController
@XRequestMapping("/test")
public class TestController {
	
	@XAutowired
	private TestService  testService;
	
	@XRequestMapping("/hello")
	public void hello(HttpServletRequest request,HttpServletResponse response ) {
		System.out.println("");
	}
}
