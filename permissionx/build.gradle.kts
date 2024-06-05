import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.vanniktech.maven.publish") version "0.28.0"
}

android {
    namespace = "com.permissionx.guolindev"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
}

mavenPublishing {
    // publishing to https://s01.oss.sonatype.org
    publishToMavenCentral(SonatypeHost.S01)
    signAllPublications()
}

mavenPublishing {
    coordinates("com.guolindev.permissionx", "permissionx", "1.8.0")

    pom {
        name.set("PermissionX")
        description.set("An open source Android library that makes handling runtime permissions extremely easy.")
        inceptionYear.set("2020")
        url.set("https://github.com/guolindev/PermissionX/")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("guolindev")
                name.set("Lin Guo")
                url.set("https://github.com/guolindev/")
            }
        }
        scm {
            url.set("https://github.com/guolindev/PermissionX/")
            connection.set("scm:git:git://github.com/guolindev/PermissionX.git")
            developerConnection.set("scm:git:ssh://git@github.com/guolindev/PermissionX.git")
        }
    }
}