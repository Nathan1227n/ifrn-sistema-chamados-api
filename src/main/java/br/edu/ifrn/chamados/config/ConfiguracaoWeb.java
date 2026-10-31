package br.edu.ifrn.chamados.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ConfiguracaoWeb implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registro) {
        registro.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest requisicao, HttpServletResponse resposta, Object handler) {
                validarInteiro(requisicao, "page", 0);
                validarInteiro(requisicao, "size", 1);
                return true;
            }
        });
    }

    private static void validarInteiro(HttpServletRequest requisicao, String parametro, int minimo) {
        String valor = requisicao.getParameter(parametro);
        if (valor != null && Integer.parseInt(valor) < minimo) {
            throw new IllegalArgumentException("Parâmetro " + parametro + " inválido");
        }
    }
}
