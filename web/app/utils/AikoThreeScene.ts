import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import { Aiko } from './Aiko'
import { AikoEnvironment } from './AikoEnvironment'
import { AikoLights } from './AikoLights'
import { PaletteShader } from './AikoPaletteShader'

export class AikoThreeScene {
    private scene: THREE.Scene
    private camera: THREE.PerspectiveCamera
    private finalCamera: THREE.PerspectiveCamera | null = null
    private renderer: THREE.WebGLRenderer
    private container: HTMLElement
    private frameId: number | null = null
    private loadingCallback: (loading: boolean) => void
    private controls: OrbitControls | null = null
    private isDebug: boolean

    private aiko: Aiko | null = null
    private environment: AikoEnvironment | null = null
    private lights: AikoLights | null = null

    // Palette Filter Pass
    private renderTarget: THREE.WebGLRenderTarget | null = null
    private postScene: THREE.Scene | null = null
    private postCamera: THREE.OrthographicCamera | null = null
    private paletteMaterial: THREE.ShaderMaterial | null = null

    constructor(container: HTMLElement, loadingCallback: (loading: boolean) => void, isDebug: boolean = false) {
        this.container = container
        this.loadingCallback = loadingCallback
        this.isDebug = isDebug

        console.log('[AikoThreeScene] initializing with palette filter...', { isDebug })

        this.scene = new THREE.Scene()
        // Use a palette color for background/fog
        this.scene.background = new THREE.Color(0x332b48) // p8
        this.scene.fog = new THREE.Fog(0x332b48, 5, 50)

        const width = this.container.clientWidth || 800
        const height = this.container.clientHeight || 600
        const aspect = width / height

        this.camera = new THREE.PerspectiveCamera(45, aspect, 0.1, 1000)
        this.camera.position.set(0, 1.5, 6)
        this.camera.lookAt(0, 1, 0)

        if (isDebug) {
            this.camera.layers.enable(1)
        }

        this.renderer = new THREE.WebGLRenderer({
            antialias: true,
            alpha: true,
            powerPreference: 'high-performance'
        })
        this.renderer.setSize(width, height)
        this.renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
        this.renderer.outputColorSpace = THREE.SRGBColorSpace
        this.container.appendChild(this.renderer.domElement)

        this.initPostProcessing(width, height)

        if (isDebug) {
            this.controls = new OrbitControls(this.camera, this.renderer.domElement)
            this.controls.enableDamping = true

            const axesHelper = new THREE.AxesHelper(10)
            axesHelper.layers.set(1)
            this.scene.add(axesHelper)

            this.finalCamera = new THREE.PerspectiveCamera(45, aspect, 0.1, 1000)
            this.finalCamera.position.set(0, 1.5, 6)
            this.finalCamera.lookAt(0, 1, 0)
        }

        this.environment = new AikoEnvironment(this.scene, isDebug)
        this.lights = new AikoLights(this.scene, isDebug)

        this.aiko = new Aiko(this.scene, this.camera, () => {
            console.log('[AikoThreeScene] aiko loaded')
            if (this.aiko?.mesh && this.lights) {
                this.lights.setSpotLightTarget(this.aiko.mesh)
            }
            this.loadingCallback(false)
        })

        this.animate()
    }

    private initPostProcessing(width: number, height: number) {
        this.renderTarget = new THREE.WebGLRenderTarget(width, height, {
            format: THREE.RGBAFormat,
            type: THREE.HalfFloatType,
            minFilter: THREE.LinearFilter,
            magFilter: THREE.LinearFilter,
            samples: 4
        })

        this.postScene = new THREE.Scene()
        this.postCamera = new THREE.OrthographicCamera(-1, 1, 1, -1, 0, 1)

        this.paletteMaterial = new THREE.ShaderMaterial({
            uniforms: THREE.UniformsUtils.clone(PaletteShader.uniforms),
            vertexShader: PaletteShader.vertexShader,
            fragmentShader: PaletteShader.fragmentShader
        })

        this.paletteMaterial.uniforms.screenSize.value.set(width, height)

        const quad = new THREE.Mesh(new THREE.PlaneGeometry(2, 2), this.paletteMaterial)
        this.postScene.add(quad)
    }

    public toggleGrid(visible: boolean) {
        if (this.environment) {
            this.environment.setGridVisible(visible)
        }
    }

    private animate = () => {
        this.frameId = requestAnimationFrame(this.animate)
        this.render()
    }

    public render() {
        const time = Date.now() * 0.001

        if (this.aiko) this.aiko.update(time)
        if (this.lights) this.lights.update()
        if (this.controls) this.controls.update()

        if (!this.renderTarget || !this.postScene || !this.postCamera || !this.paletteMaterial) {
            this.renderer.render(this.scene, this.camera)
            return
        }

        // 1. Render scene to target
        this.renderer.setRenderTarget(this.renderTarget)
        this.renderer.render(this.scene, this.camera)

        // 2. Render target to screen with palette shader
        this.renderer.setRenderTarget(null)
        this.paletteMaterial.uniforms.tDiffuse.value = this.renderTarget.texture
        this.renderer.render(this.postScene, this.postCamera)

        // Debug PiP pass (also with filter)
        if (this.isDebug && this.finalCamera) {
            const pipWidth = this.container.clientWidth * 0.25
            const pipHeight = this.container.clientHeight * 0.25
            const pipX = this.container.clientWidth - pipWidth - 20
            const pipY = 20

            // Reuse the same logic for PiP if needed, but for now we'll just render it directly
            // or we could do a second pass for PiP. 
            // Given performance, let's keep PiP unfiltered or just do a simple second pass.
            this.renderer.autoClear = false
            this.renderer.setViewport(pipX, pipY, pipWidth, pipHeight)
            this.renderer.setScissor(pipX, pipY, pipWidth, pipHeight)
            this.renderer.setScissorTest(true)

            // Render PiP to target first? No, let's keep it simple.
            this.renderer.render(this.scene, this.finalCamera)

            this.renderer.setScissorTest(false)
            this.renderer.autoClear = true
            this.renderer.setViewport(0, 0, this.container.clientWidth, this.container.clientHeight)
        }
    }

    public handleResize() {
        const w = this.container.clientWidth
        const h = this.container.clientHeight
        if (w === 0 || h === 0) return

        this.camera.aspect = w / h
        this.camera.updateProjectionMatrix()

        if (this.environment) {
            this.environment.handleResize(w, h)
        }

        this.renderer.setSize(w, h)
        this.renderTarget?.setSize(w, h)
        if (this.paletteMaterial) {
            this.paletteMaterial.uniforms.screenSize.value.set(w, h)
        }

        if (this.aiko) {
            this.aiko.handleResize()
        }

        this.render()
    }

    public destroy() {
        if (this.frameId !== null) cancelAnimationFrame(this.frameId)
        if (this.aiko) this.aiko.destroy()
        if (this.environment) this.environment.destroy()
        if (this.lights) this.lights.destroy()
        if (this.controls) this.controls.dispose()

        this.renderTarget?.dispose()
        this.paletteMaterial?.dispose()
        this.renderer.dispose()
        this.scene.clear()

        if (this.container.contains(this.renderer.domElement)) {
            this.container.removeChild(this.renderer.domElement)
        }
    }
}
