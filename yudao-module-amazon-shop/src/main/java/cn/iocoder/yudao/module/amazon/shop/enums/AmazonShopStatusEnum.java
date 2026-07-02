package cn.iocoder.yudao.module.amazon.shop.enums;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * Amazon shop status enumeration.
 *
 * <p>Lifecycle states for an Amazon shop record:
 * <pre>
 *   AUTH_PENDING  →  ENABLED     (OAuth completed successfully)
 *   ENABLED       →  AUTH_EXPIRED (access/refresh token invalidated)
 *   AUTH_EXPIRED  →  ENABLED     (re-authorised)
 *   any           →  DISABLED    (manually disabled by operator)
 * </pre>
 *
 * @author AmazonOps AI
 */
@Getter
@AllArgsConstructor
public enum AmazonShopStatusEnum implements ArrayValuable<Integer> {

    DISABLED(0,     "Disabled"),
    ENABLED(1,      "Enabled"),
    AUTH_EXPIRED(2, "Auth Expired"),
    AUTH_PENDING(3, "Auth Pending");

    /** Status code stored in the database. */
    private final Integer status;

    /** Human-readable description. */
    private final String description;

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(AmazonShopStatusEnum::getStatus)
            .toArray(Integer[]::new);

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    /**
     * Resolves an enum from its status code.
     *
     * @param status the status code
     * @return the matching enum, or {@code null} if not found
     */
    public static AmazonShopStatusEnum of(Integer status) {
        if (status == null) {
            return null;
        }
        for (AmazonShopStatusEnum s : values()) {
            if (s.status.equals(status)) {
                return s;
            }
        }
        return null;
    }
}
