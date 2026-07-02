package cn.iocoder.yudao.module.amazon.inventory.service;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class SlowMovingDetectorServiceImpl implements SlowMovingDetectorService {

    @Override
    public List<String> detectSlowMoving(Long shopId, int noSaleThreshold) {
        // TODO: 查询历史销量表，找出连续 noSaleThreshold 天无销售的 ASIN
        return Collections.emptyList();
    }
}
