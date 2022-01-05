[![codecov](https://codecov.io/gh/projectronin/interop-publishers/branch/master/graph/badge.svg?token=ayosY0YP0o&flag=aidbox-tc)](https://app.codecov.io/gh/projectronin/interop-publishers/branch/master)
[![Tests](https://github.com/projectronin/interop-publishers/actions/workflows/aidbox_testcontainer_test.yml/badge.svg)](https://github.com/projectronin/interop-publishers/actions/workflows/aidbox_testcontainer_test.yml)
[![Lint](https://github.com/projectronin/interop-publishers/actions/workflows/lint.yml/badge.svg)](https://github.com/projectronin/interop-publishers/actions/workflows/lint.yml)

# interop-aidbox-testcontainer

Provides testing utilities for Aidbox. Most notably, a Testcontainer capable of deploying an instance Aidbox within your
tests. A JUnit Extension is also provided capable of injecting YAML resources into your test instance.

## Usage

### Devbox License

In order to run Aidbox locally, you need to have a Devbox license. Information of getting a license can be
found [here](https://docs.aidbox.app/getting-started/installation/setup-aidbox.dev). After you get your license ID and
key, we need to set them in environment variables for the AidboxContainer to properly start up. Add the following to you
.zshenv file, which the appropriate values filled in:

```shell
export AIDBOX_LICENSE_ID=
export AIDBOX_LICENSE_KEY=
```

### Testcontainer

To use the Aidbox Testcontainer, simply mark up your test class with the `@TestContainer` annotation and
provide `@Container`s for the Aidbox Database and Aidbox.

```kotlin
@TestContainer
class MyTest {
    companion object {
        @Container
        val aidboxDatabaseContainer = AidboxDatabaseContainer()

        @Container
        val aidbox = AidboxContainer(aidboxDatabaseContainer)
    }
}
```

Or, you can extend the
provided [BaseAidboxTest](src/main/kotlin/com/projectronin/interop/aidbox/testcontainer/BaseAidboxTest.kt) to get the
functionality above, while also allowing yourself to inject data.

### JUnit Extension

A JUnit Extension is provided to assist with injecting data into an Aidbox instance. The extension is most easily
triggered using `@AidboxTest`, but can also be used through the `BaseAidboxTest`. This extension will then scan your
test class and methods for `@AidboxData` annotations indicating the location of resource YAML files within your test
resources.

```kotlin
@AidboxTest
@AidboxData("aidbox/patients.yaml")
class AidboxContainerClassDataTest {
    ...

    @Test
    fun `test 1`() {
        ...
    }

    @Test
    @AidboxData("/aidbox/patient1.yaml", "/aidbox/patient2.yaml")
    fun `test2`() {
        ...
    }
}
```
