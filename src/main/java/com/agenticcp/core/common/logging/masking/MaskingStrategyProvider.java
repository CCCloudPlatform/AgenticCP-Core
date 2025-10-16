package com.agenticcp.core.common.logging.masking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 마스킹 전략을 관리하는 Provider 클래스
 * 
 * 모든 MaskingStrategy Bean을 자동으로 수집하고 Map으로 관리하여
 * MaskingService가 개별 전략에 의존하지 않도록 합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class MaskingStrategyProvider {

    private final Map<MaskingType, MaskingStrategy> strategies;
    
    /**
     * 기본 전략 (DEFAULT 타입)
     */
    private final MaskingStrategy defaultStrategy;

    public MaskingStrategyProvider(List<MaskingStrategy> strategyList) {
        // 모든 MaskingStrategy Bean을 주입받아 Map으로 변환
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(
                    MaskingStrategy::getType, 
                    Function.identity(),
                    (existing, replacement) -> {
                        log.warn("중복된 마스킹 전략 발견: {} - 기존 전략을 유지합니다.", existing.getType());
                        return existing;
                    }
                ));
        
        // 기본 전략 저장
        this.defaultStrategy = strategies.get(MaskingType.DEFAULT);
        
        if (defaultStrategy == null) {
            log.error("기본 마스킹 전략(DEFAULT)을 찾을 수 없습니다");
            throw new IllegalStateException("기본 마스킹 전략이 등록되지 않았습니다.");
        }
        
        log.info("마스킹 전략 Provider 초기화 완료 - 등록된 전략 수: {}", strategies.size());
        strategies.keySet().forEach(type -> 
            log.debug("등록된 마스킹 전략: {} -> {}", type, strategies.get(type).getClass().getSimpleName())
        );
    }
    
    /**
     * 지정된 타입에 해당하는 마스킹 전략을 반환합니다.
     * 
     * @param type 마스킹 타입
     * @return 해당하는 마스킹 전략 (없으면 기본 전략 반환)
     */
    public MaskingStrategy getStrategy(MaskingType type) {
        if (type == null) {
            log.warn("마스킹 타입이 null입니다. 기본 전략을 반환합니다.");
            return defaultStrategy;
        }
        
        MaskingStrategy strategy = strategies.get(type);
        if (strategy == null) {
            log.warn("마스킹 타입 '{}'에 해당하는 전략을 찾을 수 없습니다. 기본 전략을 반환합니다.", type);
            return defaultStrategy;
        }
        
        return strategy;
    }

}
