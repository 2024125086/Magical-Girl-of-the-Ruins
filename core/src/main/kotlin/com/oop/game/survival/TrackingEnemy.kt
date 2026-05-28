package com.oop.game.survival

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.oop.game.GameObject

// 플레이어를 추적하는 적 클래스를 설정
class TrackingEnemy(
    x: Float,
    y: Float,
    private val player: SurvivalPlayer
) : GameObject(x, y, 40f, 40f) {

    // 적 이미지를 설정
    private val texture = Texture(Gdx.files.internal("enemy.png"))
    // 적의 기본 이동 속도를 설정
    private val baseSpeed = 100f
    // 현재 이동 속도를 설정 (스킬 효과 등으로 변경될 수 있음)
    private var currentSpeed = baseSpeed

    // 적의 체력을 설정
    var hp = 10
    // 적이 죽었는지 상태를 설정
    var isDead = false
    
    // --- 스킬 효과 관련 변수 ---
    // 화상 효과의 남은 시간을 계산하는 타이머를 설정
    private var burnTimer = 0f
    // 화상 효과의 전체 지속시간을 설정
    private var burnDuration = 0f

    // 객체의 생존 여부를 isDead 반전값으로 설정
    override fun isAlive(): Boolean = !isDead

    // 매 프레임 적의 상태를 갱신
    override fun update(delta: Float) {
        // 플레이어의 Shadow Veil 스킬 활성화 여부를 확인
        if (player.isShadowVeilActive) {
            // 플레이어와의 거리 제곱을 계산 (성능을 위해 제곱근 계산 생략)
            val distSq = Vector2.dst2(x, y, player.x, player.y)
            // 스킬 범위(반경 150) 안에 들어왔는지 확인
            if (distSq < 150f * 150f) {
                // 범위 안이면 속도를 절반으로 감소
                currentSpeed = baseSpeed * 0.5f
            } else {
                // 범위 밖이면 원래 속도로 복귀
                currentSpeed = baseSpeed
            }
        } else {
            // 스킬이 비활성화 상태면 원래 속도로 설정
            currentSpeed = baseSpeed
        }

        // 플레이어를 향하는 방향 벡터를 계산
        val direction = Vector2(player.x - x, player.y - y)
        // 방향 벡터를 정규화
        if (!direction.isZero) direction.nor()

        // 계산된 방향과 속도로 위치를 이동
        x += direction.x * currentSpeed * delta
        y += direction.y * currentSpeed * delta

        // 화상 효과(Ruin Flare)를 처리
        if (burnDuration > 0f) {
            // 지속시간과 타이머를 갱신
            burnDuration -= delta
            burnTimer += delta
            // 1초가 지날 때마다 화상 데미지를 줌
            if (burnTimer >= 1.0f) {
                takeDamage(2)
                burnTimer -= 1.0f
            }
        }
    }

    // 적을 화면에 그림
    override fun draw(batch: SpriteBatch) {
        // 그리기 전 기존 색상을 백업
        val oldColor = batch.color
        // 화상 상태일 경우 적의 색상을 주황색으로 변경
        if (burnDuration > 0f) batch.color = com.badlogic.gdx.graphics.Color.ORANGE
        // 화면에 적을 그림
        batch.draw(texture, x, y, width, height)
        // 기존 색상으로 복원
        batch.color = oldColor
    }

    // 객체 소멸 시 텍스처 자원을 해제
    override fun dispose() {
        texture.dispose()
    }

    // 피해를 받는 함수
    fun takeDamage(amount: Int) {
        // 체력을 감소시킴
        hp -= amount
        // 체력이 0 이하가 되면 사망 상태로 설정
        if (hp <= 0) isDead = true
    }

    // 화상 효과를 적용하는 함수
    fun applyBurn(duration: Float) {
        // 화상 지속시간과 타이머를 초기화
        burnDuration = duration
        burnTimer = 0f
    }
}