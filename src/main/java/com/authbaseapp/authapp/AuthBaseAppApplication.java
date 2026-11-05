package com.authbaseapp.authapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuthBaseAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthBaseAppApplication.class, args);
    }

}

// Docker => kontenjer => krijosh nje gje dhe ta besh run ne nje struktur
// me versione, paketa dhe os te njejte gjithmon
// Dockerr => problem punon te laptopi im
// docker image => run ne nje komp apo server => izoluar nga laptopi
// docker => aplikacione te tera + images publikuara nga provder
// image + jdk
// docker-comose.yml

// images => mysql 8.0.1
// containers => mysql 1 3306 , mysql 2 3307 , mysql 3 3308
// volum
// wsl => windows susbsitute linux system

// github => build actions => .yml
// docker compse up -d ===> pull images ==> start images


// docker desktop