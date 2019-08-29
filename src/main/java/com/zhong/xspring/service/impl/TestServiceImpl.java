package com.zhong.xspring.service.impl;

import com.zhong.xspring.annotation.XService;
import com.zhong.xspring.service.TestService;

/**
 * @author zhong
 * @date 2019年8月29日 下午1:03:45
 * 
 */
@XService
public class TestServiceImpl  implements TestService{
	@Override
	public String sayHello(String body) {
		return "Hi,"+body;
	}

}
