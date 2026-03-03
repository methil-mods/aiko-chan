import * as THREE from 'three'

export class AikoEnvironment {
    private scene: THREE.Scene
    private grid: THREE.GridHelper | null = null
    private bgTexture: THREE.Texture | null = null
    private bgPlane: THREE.Mesh | null = null

    constructor(scene: THREE.Scene, isDebug: boolean = false) {
        this.scene = scene
        this.init(isDebug)
    }

    private init(isDebug: boolean) {
        const loader = new THREE.TextureLoader()
        loader.load('/aikobase/stream_bg.webp', (texture) => {
            texture.colorSpace = THREE.SRGBColorSpace
            this.bgTexture = texture

            const geometry = new THREE.PlaneGeometry(2, 2)
            const material = new THREE.ShaderMaterial({
                uniforms: {
                    tDiffuse: { value: texture },
                    uvTransform: { value: new THREE.Matrix3() }
                },
                vertexShader: `
                    varying vec2 vUv;
                    void main() {
                        vUv = uv;
                        gl_Position = vec4(position.xy, 1.0, 1.0);
                    }
                `,
                fragmentShader: `
                    uniform sampler2D tDiffuse;
                    uniform mat3 uvTransform;
                    varying vec2 vUv;
                    void main() {
                        vec3 uv = uvTransform * vec3(vUv, 1.0);
                        gl_FragColor = texture2D(tDiffuse, uv.xy);
                    }
                `,
                depthTest: false,
                depthWrite: false
            })

            this.bgPlane = new THREE.Mesh(geometry, material)
            this.bgPlane.renderOrder = -100
            this.bgPlane.frustumCulled = false

            this.scene.add(this.bgPlane)

            // Handle initial resize if we have dimensions
            // But we'll rely on the manual handleResize call from the scene
        })

        if (isDebug) {
            this.grid = new THREE.GridHelper(20, 20, 0x444444, 0x222222)
            this.grid.position.y = -0.05
            this.grid.layers.set(1)
            this.scene.add(this.grid)
        }
    }

    public setGridVisible(visible: boolean) {
        if (this.grid) this.grid.visible = visible
    }

    public handleResize(width: number, height: number) {
        if (!this.bgPlane || !this.bgTexture || !this.bgTexture.image) return

        const material = this.bgPlane.material as THREE.ShaderMaterial
        const image = this.bgTexture.image as HTMLImageElement

        if (!image.width || !image.height) return

        const containerAspect = width / height
        const imageAspect = image.width / image.height

        let repeatX = 1
        let repeatY = 1
        let offsetX = 0
        let offsetY = 0

        if (containerAspect > imageAspect) {
            repeatY = imageAspect / containerAspect
            offsetY = (1 - repeatY) / 2
        } else {
            repeatX = containerAspect / imageAspect
            offsetX = (1 - repeatX) / 2
        }

        if (material.uniforms && material.uniforms.uvTransform) {
            const matrix = material.uniforms.uvTransform.value as THREE.Matrix3
            matrix.set(
                repeatX, 0, offsetX,
                0, repeatY, offsetY,
                0, 0, 1
            )
        }
    }

    public destroy() {
        if (this.bgTexture) this.bgTexture.dispose()
        if (this.bgPlane) {
            this.bgPlane.geometry.dispose()
            if (this.bgPlane.material instanceof THREE.Material) this.bgPlane.material.dispose()
            this.scene.remove(this.bgPlane)
        }
        if (this.grid) {
            this.grid.geometry.dispose()
            if (this.grid.material instanceof THREE.Material) this.grid.material.dispose()
            this.scene.remove(this.grid)
        }
    }
}
