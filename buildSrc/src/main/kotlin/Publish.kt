/*
 * Copyright 2021 Spotify AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.gradle.api.Project
import org.gradle.api.publish.PublicationContainer
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.extra
import org.gradle.plugins.signing.SigningExtension
import java.net.URI

const val RULER_PLUGIN_GROUP = "com.spotify.ruler"
const val RULER_PLUGIN_VERSION = "2.0.0-alpha-4" // Also adapt this version in the README

const val EXT_POM_NAME = "POM_NAME"
const val EXT_POM_DESCRIPTION = "POM_DESCRIPTION"

const val ENV_SONATYPE_USERNAME = "SONATYPE_USERNAME"
const val ENV_SONATYPE_PASSWORD = "SONATYPE_PASSWORD"

const val ENV_SIGNING_KEY = "PGP_SIGNING_KEY"
const val ENV_SIGNING_PASSWORD = "PGP_SIGNING_PASSWORD"

fun PublishingExtension.configurePublications(project: Project) {
    val javadocJar = project.tasks.register("javadocJar", Jar::class.java) {
        archiveClassifier.set("javadoc") // Use empty javadoc JAR until Dokka supports Kotlin Multiplatform projects
    }

    publications.withType(MavenPublication::class.java) {
        groupId = RULER_PLUGIN_GROUP
        version = RULER_PLUGIN_VERSION

        artifact(javadocJar)

        pom {
            name.set(project.extra[EXT_POM_NAME].toString())
            description.set(project.extra[EXT_POM_DESCRIPTION].toString())
            url.set("https://github.com/spotify/ruler")
            scm {
                url.set("https://github.com/spotify/ruler")
                connection.set("scm:git@github.com:spotify/ruler.git")
                developerConnection.set("scm:git@github.com:spotify/ruler.git")
            }
            licenses {
                license {
                    name.set("The Apache Software License, Version 2.0")
                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            developers {
                developer {
                    id.set("spotify")
                    name.set("Spotify AB")
                }
            }
        }
    }
}

fun SigningExtension.configureSigning(publications: PublicationContainer) {
    val signingKey = System.getenv(ENV_SIGNING_KEY)
    val signingPassword = System.getenv(ENV_SIGNING_PASSWORD)

    // Only sign artifacts on CI
    isRequired = signingKey != null && signingPassword != null

    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publications)
}
