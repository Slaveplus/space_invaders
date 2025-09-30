package org.newdawn.spaceinvaders.app;

import java.awt.Graphics2D;

/**
 * 공통 화면 인터페이스. 각 화면은 Canvas를 상속하는 구현체로 작성하고,
 * 수명주기(onShow/onHide), 업데이트(update), 렌더(render)를 제공합니다.
 */
public interface Screen {
    /** 화면이 표시될 때 호출 (리스너 등록 등) */
    default void onShow() {}

    /** 화면이 숨겨질 때 호출 (리스너 해제 등) */
    default void onHide() {}

    /** 화면이 최초로 생성될 때 호출 (리소스 초기화 등) */
    default void init() {}

    /**
     * 프레임 업데이트
     * @param deltaMillis 지난 프레임으로부터 경과 시간(ms)
     */
    void update(long deltaMillis);

    /**
     * 프레임 렌더링
     * @param g Graphics2D 컨텍스트 (더블버퍼 이미지를 대상으로 함)
     */
    void render(Graphics2D g);
}
