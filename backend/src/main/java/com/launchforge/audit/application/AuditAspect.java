package com.launchforge.audit.application;

import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import com.launchforge.catalog.api.dto.ProductResponse;

@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class AuditAspect {
    private final AuditWriter auditWriter;
    private final AuditMetadataFactory metadataFactory;
    private final ExpressionParser parser = new SpelExpressionParser();

    public AuditAspect(AuditWriter auditWriter, AuditMetadataFactory metadataFactory) {
        this.auditWriter = auditWriter;
        this.metadataFactory = metadataFactory;
    }

    @Around("@annotation(logAction)")
    public Object audit(ProceedingJoinPoint joinPoint, LogAction logAction) throws Throwable {
        Object result = joinPoint.proceed();
        AuditAction action = resolveAction(logAction.action(), result);
        String resourceId = evaluateResourceId(joinPoint, logAction.resourceId(), result);
        Map<String, Object> metadata = metadataFactory.create(action, joinPoint.getArgs(), result);
        auditWriter.write(action, logAction.resource(), resourceId, metadata);
        return result;
    }

    private AuditAction resolveAction(AuditAction declaredAction, Object result) {
        if (declaredAction == AuditAction.PRODUCT_UPDATED
                && result instanceof ProductResponse product
                && !product.active()) {
            return AuditAction.PRODUCT_DISABLED;
        }
        return declaredAction;
    }

    private String evaluateResourceId(ProceedingJoinPoint joinPoint, String expression, Object result) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        String[] parameterNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
        Object[] arguments = joinPoint.getArgs();
        if (parameterNames != null) {
            for (int index = 0; index < parameterNames.length; index++) {
                context.setVariable(parameterNames[index], arguments[index]);
            }
        }
        context.setVariable("result", result);
        Object value = parser.parseExpression(expression).getValue(context);
        return value == null ? null : value.toString();
    }
}
