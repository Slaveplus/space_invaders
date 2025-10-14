package org.newdawn.spaceinvaders.multyplay.sprite;

import java.awt.Graphics;
import java.awt.Image;

/**
 * 화면에 표시될 스프라이트입니다. 스프라이트는 상태 정보를 포함하지 않는다는 점에
 * 주의하세요. 즉, 이미지만 있고 위치는 없습니다. 이를 통해 이미지의 여러 복사본을
 * 저장하지 않고도 다양한 위치에서 단일 스프라이트를 사용할 수 있습니다.
 * 
 * @author Kevin Glass
 */
public class Sprite {
	/** 이 스프라이트를 위해 그려질 이미지 */
	private Image image;
	
	/**
	 * 이미지를 기반으로 새로운 스프라이트를 생성합니다
	 * 
	 * @param image 이 스프라이트인 이미지
	 */
	public Sprite(Image image) {
		this.image = image;
	}
	
	/**
	 * 그려진 스프라이트의 너비를 가져옵니다
	 * 
	 * @return 이 스프라이트의 픽셀 단위 너비
	 */
	public int getWidth() {
		return image.getWidth(null);
	}

	/**
	 * 그려진 스프라이트의 높이를 가져옵니다
	 * 
	 * @return 이 스프라이트의 픽셀 단위 높이
	 */
	public int getHeight() {
		return image.getHeight(null);
	}
	
	/**
	 * 이 스프라이트의 이미지를 가져옵니다
	 * 
	 * @return 이 스프라이트의 이미지
	 */
	public Image getImage() {
		return image;
	}
	
	/**
	 * 제공된 그래픽스 컨텍스트에 스프라이트를 그립니다
	 * 
	 * @param g 스프라이트를 그릴 그래픽스 컨텍스트
	 * @param x 스프라이트를 그릴 x 위치
	 * @param y 스프라이트를 그릴 y 위치
	 */
	public void draw(Graphics g,int x,int y) {
		g.drawImage(image,x,y,null);
	}
}