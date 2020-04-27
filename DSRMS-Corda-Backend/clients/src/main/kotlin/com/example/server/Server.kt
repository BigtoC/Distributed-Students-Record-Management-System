package com.example.server

import net.corda.client.jackson.JacksonSupport
import org.apache.catalina.servlet4preview.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.Banner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletResponse


/**
 * A Spring Boot application.
 */

@SpringBootApplication
open class Server {
    /**
     * Spring Bean that binds a Corda Jackson object-mapper to HTTP message types used in Spring.
     */
    @Bean
    open fun mappingJackson2HttpMessageConverter(@Autowired rpcConnection: NodeRPCConnection): MappingJackson2HttpMessageConverter {
        val mapper = JacksonSupport.createDefaultMapper(rpcConnection.proxy)
        val converter = MappingJackson2HttpMessageConverter()
        converter.objectMapper = mapper
        return converter
    }
}

@Component
class CorsFilter : OncePerRequestFilter() {
    override fun doFilterInternal(request: javax.servlet.http.HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        response.setHeader("Access-Control-Max-Age", "3600")
        response.setHeader("Access-Control-Allow-Origin", "*")
        response.setHeader("Access-Control-Allow-Credentials", "true")
        response.setHeader("Access-Control-Allow-Methods", "GET,HEAD,OPTIONS,POST,PUT,DELETE")
        response.setHeader("Access-Control-Allow-Headers", "Access-Control-Allow-Headers, Origin,Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers")
        filterChain.doFilter(request, response)
    }
}


@Configuration
open class CorsConfig : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:8080")
                .allowedOrigins("http://localhost:8081")
                .allowedMethods("*")
                .allowCredentials(true)
                .allowedHeaders("*");
    }
}

/**
 * Starts our Spring Boot application.
 */
fun main(args: Array<String>) {
    val app = SpringApplication(Server::class.java)
    app.setBannerMode(Banner.Mode.OFF)
    app.isWebEnvironment = true
    app.run(*args)
}
