import * as THREE from 'three'

export class Aiko {
    public mesh: THREE.Mesh | null = null
    public shadow: THREE.Mesh | null = null
    private scene: THREE.Scene
    private camera: THREE.PerspectiveCamera
    private onLoaded: () => void

    constructor(scene: THREE.Scene, camera: THREE.PerspectiveCamera, onLoaded: () => void) {
        this.scene = scene
        this.camera = camera
        this.onLoaded = onLoaded
        this.init()
    }

    private init() {
        const loader = new THREE.TextureLoader()

        const onLoad = (texture: THREE.Texture) => {
            console.log('[Aiko] texture loaded successfully')
            texture.magFilter = THREE.NearestFilter
            texture.minFilter = THREE.NearestFilter
            texture.colorSpace = THREE.SRGBColorSpace

            // Use a unit plane geometry (1x1) and scale it instead of recreating
            const geometry = new THREE.PlaneGeometry(1, 1)
            const material = new THREE.MeshBasicMaterial({
                map: texture,
                transparent: true,
                side: THREE.DoubleSide,
                fog: false
            })
            this.mesh = new THREE.Mesh(geometry, material)
            this.mesh.position.y = 1.0

            // Initial scale
            this.updateScale()

            this.scene.add(this.mesh)
            this.onLoaded()
        }

        const onError = (err: any) => {
            console.error('[Aiko] failed to load texture:', err)
            this.onLoaded()
        }

        loader.load('/aikobase/stream_ame_comic_000.png', onLoad, undefined, onError)
    }

    private updateScale() {
        if (!this.mesh || !this.camera) return

        const vFOV = (this.camera.fov * Math.PI) / 180
        const visibleHeight = 2 * Math.tan(vFOV / 2) * this.camera.position.z

        const zoomFactor = 1.1
        const height = visibleHeight * zoomFactor
        const width = height * 1.55 // Maintain aspect ratio

        this.mesh.scale.set(width, height, 1)
    }

    public update(time: number) {
        if (!this.mesh) return
        this.mesh.rotation.y = Math.sin(time * 0.3) * 0.04
    }

    public handleResize() {
        this.updateScale()
    }

    public destroy() {
        if (this.mesh) {
            this.mesh.geometry.dispose()
            if (this.mesh.material instanceof THREE.Material) {
                this.mesh.material.dispose()
            }
            if (this.mesh.material instanceof THREE.MeshBasicMaterial && this.mesh.material.map) {
                this.mesh.material.map.dispose()
            }
            this.scene.remove(this.mesh)
        }
    }
}
