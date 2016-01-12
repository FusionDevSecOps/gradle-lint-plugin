package com.netflix.nebula.lint.plugin

import nebula.test.IntegrationSpec

class GradleLintPluginSpec extends IntegrationSpec {
    def 'run multiple rules on a single module project'() {
        when:
        buildFile << """
            apply plugin: 'java'
            apply plugin: ${GradleLintPlugin.name}

            gradleLint.rules = ['dependency-parentheses', 'dependency-tuple']

            dependencies {
                compile('com.google.guava:guava:18.0')
                testCompile group: 'junit',
                    name: 'junit',
                    version: '4.11'
            }
        """

        then:
        def results = runTasksSuccessfully('assemble')

        when:
        def console = results.standardOutput.readLines()

        then:
        console.findAll { it.startsWith('warning') }.size() == 2
        console.any { it.contains('dependency-parentheses') }
        console.any { it.contains('dependency-tuple') }
    }

    def 'run rules on multi-module project where one of the subprojects has no build.gradle'() {
        when:
        buildFile << """
            allprojects {
                apply plugin: 'java'
                apply plugin: ${GradleLintPlugin.name}
                gradleLint.rules = ['dependency-parentheses', 'dependency-tuple']
            }
        """

        addSubproject('sub')

        then:
        runTasksSuccessfully('gradleLint')
    }

    def 'auto correct all violations on a single module project'() {
        when:
        buildFile << """
            apply plugin: ${GradleLintPlugin.name}
            apply plugin: 'java'

            gradleLint.rules = ['dependency-parentheses', 'dependency-tuple']

            dependencies {
                compile('com.google.guava:guava:18.0')
            }
        """

        then:
        def results = runTasksSuccessfully('fixGradleLint')
        println results.standardOutput

        buildFile.text.contains("""
            apply plugin: ${GradleLintPlugin.name}
            apply plugin: 'java'

            gradleLint.rules = ['dependency-parentheses', 'dependency-tuple']

            dependencies {
                compile 'com.google.guava:guava:18.0'
            }
        """.toString())
    }

    def 'rules relative to each project'() {
        when:
        buildFile << """
            allprojects {
                apply plugin: ${GradleLintPlugin.name}
                gradleLint.rules = ['dependency-parentheses', 'dependency-tuple']
            }

            subprojects {
                apply plugin: 'java'
                dependencies {
                    compile('com.google.guava:guava:18.0')
                }
            }
        """

        addSubproject('sub', """
            dependencies {
                testCompile group: 'junit',
                    name: 'junit',
                    version: '4.11'
            }

            task taskA {}
        """)

        then:
        def results = runTasksSuccessfully('taskA')

        when:
        def console = results.standardOutput.readLines()

        then:
        console.findAll { it.startsWith('warning') }.size() == 2
        console.any { it.contains('dependency-parentheses') }
        console.any { it.contains('dependency-tuple') }
    }

    def 'generate a lint report for a single module project'() {
        when:
        buildFile << """
            apply plugin: ${GradleLintPlugin.name}
            apply plugin: 'java'

            gradleLint.rules = ['dependency-parentheses', 'dependency-tuple']

            dependencies {
                compile('com.google.guava:guava:18.0')
                testCompile group: 'junit',
                    name: 'junit',
                    version: '4.11'
            }
        """

        then:
        def results = runTasksSuccessfully('generateGradleLintReport')

        when:
        def console = results.standardOutput.readLines()

        then:
        console.findAll { it.startsWith('warning') }.size() == 2
        console.any { it.contains('dependency-parentheses') }
        console.any { it.contains('dependency-tuple') }
        new File(projectDir, "build/reports/gradleLint/${moduleName}.html").exists()
    }
}
