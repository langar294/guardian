package com.landao.guardian.annotations.author;


import com.landao.guardian.enums.LogicType;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD,ElementType.TYPE})
public @interface RequiredPermission {

    @AliasFor("permissions")
    String[] value() default {};

    @AliasFor("value")
    String[] permissions() default {};

    /**
     * 不能使用LogicType.Not,请在数据库中设计!user:delete来实现对应的功能
     */
    LogicType logicType() default LogicType.And;

}
