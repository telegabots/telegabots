package org.github.telegabots.service

import org.github.telegabots.api.MessageSender
import org.github.telegabots.api.Service
import org.github.telegabots.api.ServiceProvider
import org.github.telegabots.api.UserService
import org.github.telegabots.api.config.BotConfig
import org.github.telegabots.test.mockStrict
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.verify
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.function.Supplier
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/**
 * Test class for [InternalServiceProvider].
 */
class InternalServiceProviderTest {
    /**
     * Test method for [InternalServiceProvider.tryGetService].
     */
    @Test
    fun `getService must return the same instance`() {
        val userProviderMock = mockStrict(ServiceProvider::class.java)
        doReturn(TestService()).`when`(userProviderMock).tryGetService(TestService::class.java)
        val serviceProvider: ServiceProvider = createProvider(userProviderMock)

        val service1 = serviceProvider.tryGetService(TestService::class.java)
        assertNotNull(service1)
        val service2 = serviceProvider.tryGetService(TestService::class.java)
        assertSame(service1, service2)

        verify(userProviderMock).tryGetService(TestService::class.java)

        // check when service is not found
        doReturn(null).`when`(userProviderMock).tryGetService(TestService2::class.java)
        val service3 = serviceProvider.tryGetService(TestService2::class.java)
        assertNull(service3)

        val ex = assertThrows<IllegalStateException> { serviceProvider.getService(TestService2::class.java) }
        assertEquals("Service not found: org.github.telegabots.service.TestService2", ex.message)
    }

    /**
     * Test method for [InternalServiceProvider.tryGetUserService].
     */
    @Test
    fun `getUserService must return the same instance`() {
        val userProviderMock = mockStrict(ServiceProvider::class.java)
        doReturn(UserTestService()).`when`(userProviderMock).tryGetUserService(UserTestService::class.java, 42L)
        val serviceProvider: ServiceProvider = createProvider(userProviderMock)

        val service1 = serviceProvider.tryGetUserService(UserTestService::class.java, 42L)
        assertNotNull(service1)
        val service2 = serviceProvider.tryGetUserService(UserTestService::class.java, 42L)
        assertSame(service1, service2)

        verify(userProviderMock).tryGetUserService(UserTestService::class.java, 42L)

        // check when service is not found
        doReturn(null).`when`(userProviderMock).tryGetUserService(UserTestService::class.java, 43L)
        val service3 = serviceProvider.tryGetUserService(UserTestService::class.java, 43L)
        assertNull(service3)

        val ex = assertThrows<IllegalStateException> { serviceProvider.getUserService(UserTestService::class.java, 43L) }
        assertEquals("User service not found: org.github.telegabots.service.UserTestService", ex.message)
    }

    /**
     * Test method for [InternalServiceProvider.tryGetSupplier].
     */
    @Test
    fun `getSupplier must return the same instance`() {
        val userProviderMock = mockStrict(ServiceProvider::class.java)
        doReturn(Supplier { LocalDateTime.now() }).`when`(userProviderMock)
            .tryGetSupplier(LocalDateTime::class.java)
        val serviceProvider: ServiceProvider = createProvider(userProviderMock)

        val supplier1 = serviceProvider.tryGetSupplier(LocalDateTime::class.java)
        val supplier2 = serviceProvider.tryGetSupplier(LocalDateTime::class.java)
        assertSame(supplier1!!, supplier2!!)
        val time1 = supplier1.get()
        Thread.sleep(200)
        val time2 = supplier1.get()
        assertNotEquals(time1, time2, "Supplier can return different values")

        verify(userProviderMock).tryGetSupplier(LocalDateTime::class.java)

        // check when supplier is not found
        doReturn(null).`when`(userProviderMock).tryGetSupplier(OffsetDateTime::class.java)
        val supplier3 = serviceProvider.tryGetSupplier(OffsetDateTime::class.java)
        assertNull(supplier3)

        val ex = assertThrows<IllegalStateException> { serviceProvider.getSupplier(OffsetDateTime::class.java) }
        assertEquals("Supplier<java.time.OffsetDateTime> not found", ex.message)
    }

    private fun createProvider(userProvider: ServiceProvider): ServiceProvider {
        val jsonService = mockStrict(JsonService::class.java)
        val config = mockStrict(BotConfig::class.java)
        val messageSender = mockStrict(MessageSender::class.java)
        val serviceProvider: ServiceProvider = InternalServiceProvider(userProvider, messageSender, jsonService, config)
        return serviceProvider
    }
}

open class TestService : Service

open class TestService2 : Service

open class UserTestService : UserService {
    override fun userId(): Long = 42L
}
