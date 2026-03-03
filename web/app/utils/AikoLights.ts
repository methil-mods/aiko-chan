import * as THREE from 'three'

export class AikoLights {
    private scene: THREE.Scene
    public ambientLight: THREE.AmbientLight | null = null
    public pointLight: THREE.PointLight | null = null
    public spotLight: THREE.SpotLight | null = null

    // Helpers
    private pointLightHelper: THREE.PointLightHelper | null = null
    private spotLightHelper: THREE.SpotLightHelper | null = null

    constructor(scene: THREE.Scene, enableHelpers: boolean = false) {
        this.scene = scene
        this.init(enableHelpers)
    }

    private init(enableHelpers: boolean) {
        this.ambientLight = new THREE.AmbientLight(0xffffff, 0.6)
        this.scene.add(this.ambientLight)

        this.pointLight = new THREE.PointLight(0xff69b4, 1.6)
        this.pointLight.position.set(2, 4, 4)
        this.scene.add(this.pointLight)

        // Dedicated light for Aiko
        this.spotLight = new THREE.SpotLight(0xffffff, 20)
        this.spotLight.position.set(2, -5, -5)
        this.spotLight.angle = Math.PI / 6
        this.spotLight.penumbra = 1
        this.spotLight.decay = 1
        this.spotLight.distance = 50
        this.scene.add(this.spotLight)

        if (enableHelpers) {
            this.pointLight.distance = 50
            this.pointLightHelper = new THREE.PointLightHelper(this.pointLight, 0.5)
            // Put helpers on debug layer (1)
            this.pointLightHelper.layers.set(1)
            this.scene.add(this.pointLightHelper)

            this.spotLightHelper = new THREE.SpotLightHelper(this.spotLight)
            this.spotLightHelper.layers.set(1)
            this.scene.add(this.spotLightHelper)
        }
    }

    public update() {
        if (this.spotLightHelper) {
            this.spotLightHelper.update()
        }
    }

    public setSpotLightTarget(target: THREE.Object3D) {
        if (this.spotLight) {
            this.spotLight.target = target
            if (this.spotLightHelper) this.spotLightHelper.update()
        }
    }

    public destroy() {
        if (this.ambientLight) this.scene.remove(this.ambientLight)
        if (this.pointLight) this.scene.remove(this.pointLight)
        if (this.spotLight) this.scene.remove(this.spotLight)
        if (this.pointLightHelper) this.scene.remove(this.pointLightHelper)
        if (this.spotLightHelper) this.scene.remove(this.spotLightHelper)
    }
}
