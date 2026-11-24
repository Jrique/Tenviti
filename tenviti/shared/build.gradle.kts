import com.android.build.gradle.LibraryExtension
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Contratos da API (DTOs, enums e regras de formato) usados pelo servidor, pelo app e pelos testes.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlin.serialization)
}

// No build da imagem do servidor (-PsomenteServidor=true) não há Android SDK: só o alvo JVM é configurado.
val somenteServidor = providers.gradleProperty("somenteServidor").orNull == "true"
if (!somenteServidor) apply(plugin = "com.android.library")

kotlin {
    jvm()
    if (!somenteServidor) {
        androidTarget {
            compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
        }
        js { browser() }
        @OptIn(ExperimentalWasmDsl::class)
        wasmJs { browser() }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

if (!somenteServidor) {
    extensions.configure<LibraryExtension> {
        namespace = "br.com.tenviti.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
    }
}
