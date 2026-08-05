package com.org73n37.crudapp.infrastructure.annotations;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Regression tests for the {@link CrudResource} RBAC contract.
 *
 * <p>The engine is intended to be deny-by-default: a resource that does not
 * explicitly declare {@code roles()} must expose <em>no</em> roles, so a
 * forgotten or unconfigured annotation can never accidentally expose data.
 */
class CrudResourceRbacTest {

    @CrudResource(path = "defaults", dto = Object.class)
    private static final class DefaultRolesResource {}

    @CrudResource(path = "explicit", dto = Object.class, roles = {"ADMIN"})
    private static final class ExplicitRolesResource {}

    @Test
    void rolesDefaultToEmptyDenyByDefault() {
        CrudResource annotation = DefaultRolesResource.class.getAnnotation(CrudResource.class);
        assertArrayEquals(new String[] {}, annotation.roles(),
                "@CrudResource.roles() must default to an empty array (deny-by-default)");
    }

    @Test
    void explicitRolesAreHonored() {
        CrudResource annotation = ExplicitRolesResource.class.getAnnotation(CrudResource.class);
        assertArrayEquals(new String[] {"ADMIN"}, annotation.roles(),
                "Explicitly declared roles must be preserved");
    }

    @Test
    void defaultsForOtherAttributesAreUnchanged() {
        CrudResource annotation = DefaultRolesResource.class.getAnnotation(CrudResource.class);
        assertEquals("v1", annotation.version());
    }
}
