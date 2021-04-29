package com.power.supplybelt;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
@ComponentScan(basePackages="com.power.supplybelt")
public class SwaggerConfig {
    /*@Configuration 相当于是我们的配置文件
    @EnableWebMvc
    @EnableSwagger2 使用swagger
    @ComponentScan  扫描包路径
    @Bean   相当于配置一个bean
    * */
    @Bean
    public Docket api(){
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(this.apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.power.supplybelt"))
                .paths(PathSelectors.any())
                .build();
    }


    private ApiInfo apiInfo(){
        @SuppressWarnings("deprecation")
        ApiInfo info=new ApiInfo(
                "Spring 构建Restful",
                "swagger show",
                "aa",
                "a",
                "willow",
                "license",
                "x");
        return info;
    }
}
