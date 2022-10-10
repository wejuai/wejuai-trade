package com.wejuai.trade.core.config;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.trace.http.HttpTraceRepository;
import org.springframework.boot.actuate.trace.http.InMemoryHttpTraceRepository;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author ZM.Wang
 */
@Configuration
public class HttpTraceActuatorConfig {

    @Bean
    public HttpTraceRepository httpTraceRepository() {
        return new InMemoryHttpTraceRepository();
    }

    @Bean
    ActuatorSecurityFilter actuatorSecurityFilter() {
        return new ActuatorSecurityFilter();
    }

    @Bean
    FilterRegistrationBean<ActuatorSecurityFilter> actuatorSecurityFilterRegistration(ActuatorSecurityFilter actuatorSecurityFilter) {
        FilterRegistrationBean<ActuatorSecurityFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(actuatorSecurityFilter);
        registration.addUrlPatterns("/actuator/httptrace");
        return registration;
    }

    public static class ActuatorSecurityFilter implements Filter {

        private static final Logger logger = LoggerFactory.getLogger(ActuatorSecurityFilter.class);

        @Override
        public void init(FilterConfig filterConfig) {
            logger.info("actuator安全框架启动");
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
            HttpServletRequest servletRequest = (HttpServletRequest) request;
            if (!StringUtils.equals("xxxxxx", servletRequest.getParameter("username"))
                    || !StringUtils.equals("xxxxxx", servletRequest.getParameter("password"))) {
                String ip = servletRequest.getHeader("X-Forwarded-For");
                if (StringUtils.isBlank(ip)) {
                    ip = request.getRemoteAddr();
                }
                logger.warn("有人访问httptrace: " + ip);
                HttpServletResponse servletResponse = (HttpServletResponse) response;
                servletResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            chain.doFilter(request, response);
        }

        @Override
        public void destroy() {
        }
    }

}