package com.oop.game.survival

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.oop.game.GameObject

// 투사체(총알) 클래스를 설정
class Projectile(
    x: Float,
    y: Float,
    targetDirection: Vector2,
    private val worldWidth: Float,
    private val worldHeight: Float
) : GameObject(x, y, 10f, 10f) {

    // 투사체 이미지를 tile.png로 임시 설정
    private val texture = Texture(Gdx.files.internal("tile.png")) 
    // 투사체의 날아가는 속도를 설정
    private val speed = 400f
    // 날아갈 방향을 길이가 1인 벡터로 정규화하여 설정
    private val direction = targetDirection.nor() 

    // 투사체가 사라져야 할 상태인지 나타내는 변수를 설정
    var isDead = false

    // 객체가 살아있는지 여부를 isDead 변수 반전값으로 설정
    override fun isAlive(): Boolean = !isDead

    // 매 프레임마다 투사체의 위치를 갱신
    override fun update(delta: Float) {
        // 정해진 방향과 속도, 시간에 따라 x 좌표를 이동
        x += direction.x * speed * delta
        // 정해진 방향과 속도, 시간에 따라 y 좌표를 이동
        y += direction.y * speed * delta

        // 투사체가 월드 밖으로 벗어났는지 확인
        if (x < 0 || x > worldWidth || y < 0 || y > worldHeight) {
            // 월드 밖으로 나가면 삭제 상태로 설정
            isDead = true
        }
    }

    // 투사체를 화면에 그리는 함수
    override fun draw(batch: SpriteBatch) {
        // 원래 설정된 색상을 임시 저장
        val oldColor = batch.color
        // 투사체 색상을 노란색으로 설정
        batch.color = com.badlogic.gdx.graphics.Color.YELLOW
        // 화면에 투사체 텍스처를 지정된 크기와 위치에 그림
        batch.draw(texture, x, y, width, height)
        // 그리기 작업이 끝난 후 원래 색상으로 되돌림
        batch.color = oldColor
    }

    // 객체가 소멸할 때 메모리에서 텍스처를 해제하도록 설정
    override fun dispose() {
        texture.dispose()
    }
}