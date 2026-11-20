import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.spring") version "2.3.0"
    kotlin("plugin.jpa") version "2.3.0"
    kotlin("plugin.allopen") version "2.3.0"
    kotlin("kapt") version "2.3.0"
    id("org.springframework.boot") version "4.0.4"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.projectx"
version = "0.0.1"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

// Kotlin 2.3.0의 jpa 플러그인은 no-arg를 제공한다. 프록시용 open은 따로 설정한다.
allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-liquibase")
    implementation("org.springframework.boot:spring-boot-starter-kafka")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.github.openfeign.querydsl:querydsl-jpa:6.11")
    kapt("io.github.openfeign.querydsl:querydsl-apt:6.11:jakarta")
    kapt("jakarta.persistence:jakarta.persistence-api")
    kapt("jakarta.annotation:jakarta.annotation-api")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

springBoot {
    mainClass.set("com.projectx.kotlinlab.KotlinLabMainKt")
}

tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("http-smoke")
    }
}

// 실행 중인 로컬 실습 앱을 Kotlin/JUnit으로 점검한다. 일반 test와 별도로 실행한다.
tasks.register<Test>("httpSmokeTest") {
    group = "verification"
    description = "실행 중인 학습 앱의 DB·HTTP·선택 Kafka 동작을 확인합니다."
    dependsOn(tasks.named("testClasses"))
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        includeTags("http-smoke")
    }
    systemProperty("lab.baseUrl", providers.gradleProperty("labBaseUrl").getOrElse("http://127.0.0.1:58080"))
    systemProperty("lab.kafka.check", providers.gradleProperty("labKafka").getOrElse("false"))
    testLogging.showStandardStreams = true
    // 대상 앱·DB의 상태는 Gradle 입력 파일만으로 추적할 수 없어 매번 실제 요청한다.
    outputs.upToDateWhen { false }
}
