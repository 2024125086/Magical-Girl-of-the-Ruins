package com.oop.game.survival

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.oop.game.GameObject

// 범위 피해를 주는 폭발 이펙트 클래스를 설정
class Explosion(
    centerX: Float,
    centerY: Float,
    val radius: Float,
    val damage: Int,
    val duration: Float
) : GameObject(centerX - radius, centerY - radius, radius * 2, radius * 2) {

    // 폭발이 지속된 시간을 저장하는 타이머 변수를 설정
    private var timer = 0f
    // 폭발 객체가 사라져야 할 상태인지 나타내는 변수를 설정
    var isDead = false
    // 폭발 이펙트로 사용할 임시 이미지를 설정
    private val texture = Texture(Gdx.files.internal("tile.png"))

    // 폭발에 맞은 적을 기억하여 중복 데미지를 주지 않도록 저장하는 목록을 설정
    val hitEnemies = mutableSetOf<GameObject>()

    // 객체의 생존 여부를 isDead 반전값으로 설정
    override fun isAlive() = !isDead

    // 매 프레임마다 폭발 상태를 갱신
    override fun update(delta: Float) {
        // 지속된 시간에 delta를 더해 타이머를 갱신
        timer += delta
        // 지속 시간이 폭발 수명을 넘기면 삭제 상태로 설정
        if (timer >= duration) {
            isDead = true
        }
    }

    // 폭발 이펙트를 화면에 그림
    override fun draw(batch: SpriteBatch) {
        // 그리기 전 기존 색상을 백업하여 설정
        val oldColor = batch.color
        // 시간이 지날수록 점점 투명해지도록 알파 값을 설정
        val alpha = 1f - (timer / duration)
        // 폭발 색상을 반투명한 빨간색으로 설정
        batch.color = Color(1f, 0.2f, 0.2f, alpha * 0.7f)
        // 화면에 설정한 색상으로 폭발 영역을 그림
        batch.draw(texture, x, y, width, height)
        // 기존 색상으로 원상 복구하도록 설정
        batch.color = oldColor
    }

    // 객체 소멸 시 텍스처 자원을 메모리에서 해제하도록 설정
    override fun dispose() {
        texture.dispose()
    }
}

// Void Shift 스킬을 썼을 때 남는 잔상 이펙트 클래스를 설정
class VoidShadow(
    x: Float,
    y: Float
) : GameObject(x, y, 30f, 30f) {

    // 잔상이 유지된 시간을 저장하는 타이머 변수를 설정
    private var timer = 0f
    // 잔상이 사라져야 할 상태인지 나타내는 변수를 설정
    var isDead = false
    // 잔상 이미지로 플레이어 이미지를 설정
    private val texture = Texture(Gdx.files.internal("player.png"))

    // 객체의 생존 여부를 isDead 반전값으로 설정
    override fun isAlive() = !isDead

    // 매 프레임마다 잔상의 상태를 갱신
    override fun update(delta: Float) {
        // 지속된 시간에 delta를 더해 타이머를 갱신
        timer += delta
        // 잔상 유지 시간이 1초가 넘어가면 삭제 상태로 설정 (이후 폭발 유발)
        if (timer >= 1.0f) {
            isDead = true
        }
    }

    // 잔상 이펙트를 화면에 그림
    override fun draw(batch: SpriteBatch) {
        // 그리기 전 기존 색상을 백업하여 설정
        val oldColor = batch.color
        // 잔상 색상을 반투명한 보라색으로 설정
        batch.color = Color(0.5f, 0f, 0.8f, 0.6f) 
        // 화면에 잔상을 그림
        batch.draw(texture, x, y, width, height)
        // 기존 색상으로 원상 복구하도록 설정
        batch.color = oldColor
    }

    // 객체 소멸 시 텍스처 자원을 메모리에서 해제하도록 설정
    override fun dispose() {
        texture.dispose()
    }
}