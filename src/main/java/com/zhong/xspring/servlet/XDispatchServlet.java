package com.zhong.xspring.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zhong.xspring.annotation.XAutowired;
import com.zhong.xspring.annotation.XController;
import com.zhong.xspring.annotation.XRequestMapping;
import com.zhong.xspring.annotation.XService;

/**
 * Servlet implementation class XDispatchServlet
 */
public class XDispatchServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * 配置文件
	 */
	private Properties contextConfig = new Properties();

	private List<String> classNameList = new ArrayList<>();

	/**
	 * IOC 容器
	 */
	Map<String, Object> iocMap = new HashMap<String, Object>();

	Map<String, Method> handlerMapping = new HashMap<String, Method>();

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public XDispatchServlet() {
		super();
	}

	/**
	 * @see Servlet#init(ServletConfig)
	 */
	public void init(ServletConfig config) throws ServletException {
		// 1加载配置
		doLoadConfig(config.getInitParameter("contextConfigLocation"));
		// 2扫描包
		doScanner(contextConfig.getProperty("scan-package"));

		// 3实例化
		doInstance();

		// 4依赖注入
		doAutowired();

		// 5初始化映射
		initHandlerMapping();
	}

	private void initHandlerMapping() {
		if (iocMap.isEmpty()) {
			return;
		}

		for (Map.Entry<String, Object> entry : iocMap.entrySet()) {
			Class<?> clazz = entry.getValue().getClass();

			if (!clazz.isAnnotationPresent(XController.class)) {
				continue;
			}

			String baseUrl = "";

			if (clazz.isAnnotationPresent(XRequestMapping.class)) {
				XRequestMapping xRequestMapping = clazz.getAnnotation(XRequestMapping.class);
				baseUrl = xRequestMapping.value();
			}

			for (Method method : clazz.getMethods()) {
				if (!method.isAnnotationPresent(XRequestMapping.class)) {
					continue;
				}

				XRequestMapping xRequestMapping = method.getAnnotation(XRequestMapping.class);

				String url = ("/" + baseUrl + "/" + xRequestMapping.value()).replaceAll("/+", "/");

				handlerMapping.put(url, method);
			}
		}
	}

	private void doAutowired() {
		if (iocMap.isEmpty()) {
			return;
		}

		for (Map.Entry<String, Object> entry : iocMap.entrySet()) {

			// 获得某个类的所有声明的字段，即包括public、private和proteced，但是不包括父类的生命字段
			Field[] fields = entry.getValue().getClass().getDeclaredFields();

			for (Field field : fields) {
				if (!field.isAnnotationPresent(XAutowired.class)) {
					continue;
				}

				// 获取注解对应的类
				XAutowired xAutowired = field.getAnnotation(XAutowired.class);
				String beanName = xAutowired.value().trim();

				// 获取 XAutowired 注解的值
				if ("".equals(beanName)) {
					beanName = field.getType().getName();
				}

				// 只要加了注解，都要加载，不管是 private 还是 protect
				field.setAccessible(true);

				try {
					field.set(entry.getValue(), iocMap.get(beanName));
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}

	}

	private void doInstance() {

		if (classNameList.isEmpty()) {
			return;
		}
		try {
			for (String className : classNameList) {
				Class<?> clazz = Class.forName(className);
				if (clazz.isAnnotationPresent(XController.class)) {
					String beanName = toLowerFirstCase(clazz.getSimpleName());
					Object instance = clazz.newInstance();
					// 保存在 ioc 容器
					iocMap.put(beanName, instance);
				} else if (clazz.isAnnotationPresent(XService.class)) {
					String beanName = toLowerFirstCase(clazz.getSimpleName());
					// 如果注解包含自定义名称, 例如 @Service("testService")
					XService xService = clazz.getAnnotation(XService.class);
					if (!"".equals(xService.value())) {
						beanName = xService.value();
					}
					Object instance = clazz.newInstance();
					iocMap.put(beanName, instance);

					// getInterfaces 此方法返回这个类中实现接口的数组
					for (Class<?> i : clazz.getInterfaces()) {
						if (iocMap.containsKey(i.getName())) {
							throw new Exception("The Bean Name Is Exist.");
						}
						// 接口不能实例化，接口存实现类的实例
						iocMap.put(i.getName(), instance);
					}
				}
			}
		} catch (Exception e) {
		}

	}

	/**
	 * 获取类的首字母小写的名称
	 *
	 * @param className
	 *            ClassName
	 * @return java.lang.String
	 */
	private String toLowerFirstCase(String className) {
		char[] charArray = className.toCharArray();
		charArray[0] += 32;
		return String.valueOf(charArray);
	}

	private void doScanner(String scanPackage) {
		// . 转化为 /
		URL resourcePath = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
		/**
		 * 递归出口
		 */
		if (resourcePath == null) {
			return;
		}

		// 或 filePath 对应的 File 对象
		File classPath = new File(resourcePath.getFile());
		for (File file : classPath.listFiles()) {
			if (file.isDirectory()) {
				// 子目录递归
				doScanner(scanPackage + "." + file.getName());
			} else {
				if (!file.getName().endsWith(".class")) {
					continue;
				}
				// 转换成 com.zhong.TestController 的形式
				String className = (scanPackage + "." + file.getName()).replace(".class", "");
				// 保存在内容
				classNameList.add(className);
			}
		}

	}

	/**
	 * 1.加载配置
	 * 
	 * @param contextConfigLocation
	 */
	private void doLoadConfig(String contextConfigLocation) {
		InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
		try {
			contextConfig.load(resourceAsStream);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (resourceAsStream != null) {
				try {
					resourceAsStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 运行阶段，进行拦截，匹配
	 *
	 * @param req
	 *            请求
	 * @param resp
	 *            响应
	 */
	private void doDispatch(HttpServletRequest req, HttpServletResponse resp)
			throws InvocationTargetException, IllegalAccessException {

		String url = req.getRequestURI();
		String contextPath = req.getContextPath();
		url = url.replaceAll(contextPath, "").replaceAll("/+", "/");
		System.out.println("[INFO-7] request url-->" + url);

		if (!this.handlerMapping.containsKey(url)) {
			try {
				resp.getWriter().write("404 NOT FOUND!!");
				return;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		Method method = this.handlerMapping.get(url);

		System.out.println("[INFO-7] method-->" + method);

		String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());

		System.out.println("[INFO-7] iocMap.get(beanName)->" + iocMap.get(beanName));

		// 第一个参数是获取方法，后面是参数，多个参数直接加，按顺序对应
		method.invoke(iocMap.get(beanName), req, resp);

		System.out.println("[INFO-7] method.invoke put {" + iocMap.get(beanName) + "}.");
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// 7、运行阶段
		try {
			doDispatch(req, resp);
		} catch (Exception e) {
			e.printStackTrace();
			resp.getWriter().write("500 Exception Detail:\n" + Arrays.toString(e.getStackTrace()));
		}

	}

}
