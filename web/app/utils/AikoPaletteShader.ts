import * as THREE from 'three'

export const PaletteShader = {
    uniforms: {
        tDiffuse: { value: null },
        palette: {
            value: [
                new THREE.Vector3(0.8314, 0.9804, 1.0),    // p1 (Lightest)
                new THREE.Vector3(0.8549, 0.7294, 1.0),    // p2
                new THREE.Vector3(0.9882, 0.6078, 0.8275), // p3
                new THREE.Vector3(0.7373, 0.5529, 1.0),    // p4
                new THREE.Vector3(0.5373, 0.4667, 0.7882), // p5
                new THREE.Vector3(0.2980, 0.3451, 0.4941), // p6
                new THREE.Vector3(0.2353, 0.2510, 0.3490), // p7
                new THREE.Vector3(0.2000, 0.1686, 0.2824)  // p8 (Darkest)
            ]
        },
        opacity: { value: 1.0 },
        ditherEnabled: { value: 1.0 },
        screenSize: { value: new THREE.Vector2(800, 600) }
    },

    vertexShader: `
        varying vec2 vUv;
        void main() {
            vUv = uv;
            gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
        }
    `,

    fragmentShader: `
        uniform sampler2D tDiffuse;
        uniform vec3 palette[8];
        uniform float opacity;
        uniform float ditherEnabled;
        uniform vec2 screenSize;
        varying vec2 vUv;

        // 4x4 Bayer Dither Matrix
        float bayer4x4(vec2 uv) {
            vec2 pixel = floor(uv * screenSize);
            int x = int(mod(pixel.x, 4.0));
            int y = int(mod(pixel.y, 4.0));
            
            if (y == 0) {
                if (x == 0) return 0.0/16.0; if (x == 1) return 8.0/16.0; if (x == 2) return 2.0/16.0; if (x == 3) return 10.0/16.0;
            } else if (y == 1) {
                if (x == 0) return 12.0/16.0; if (x == 1) return 4.0/16.0; if (x == 2) return 14.0/16.0; if (x == 3) return 6.0/16.0;
            } else if (y == 2) {
                if (x == 0) return 3.0/16.0; if (x == 1) return 11.0/16.0; if (x == 2) return 1.0/16.0; if (x == 3) return 9.0/16.0;
            } else if (y == 3) {
                if (x == 0) return 15.0/16.0; if (x == 1) return 7.0/16.0; if (x == 2) return 13.0/16.0; if (x == 3) return 5.0/16.0;
            }
            return 0.0;
        }

        void main() {
            vec4 texel = texture2D(tDiffuse, vUv);
            vec3 color = texel.rgb;
            
            // Apply dithering before quantization
            if (ditherEnabled > 0.5) {
                float dither = bayer4x4(vUv) - 0.5;
                color += dither * 0.15; // Adjustment factor for 8-color palette
            }

            // Find nearest color in palette
            vec3 nearestColor = palette[0];
            float minDist = distance(color, palette[0]);
            
            for (int i = 1; i < 8; i++) {
                float dist = distance(color, palette[i]);
                if (dist < minDist) {
                    minDist = dist;
                    nearestColor = palette[i];
                }
            }
            
            gl_FragColor = vec4(nearestColor, texel.a * opacity);
        }
    `
}
