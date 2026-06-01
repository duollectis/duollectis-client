#version 330

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
	vec2 OutSize;
	vec2 InSize;
};

layout(std140) uniform BloomExtractConfig {
	float Threshold;
	float Intensity;
};

in vec2 texCoord;

out vec4 fragColor;

void main() {
	vec4 color = texture(InSampler, texCoord);
	float brightness = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
	float extracted = max(brightness - Threshold, 0.0) / max(1.0 - Threshold, 0.0001);
	fragColor = vec4(color.rgb * extracted * Intensity, color.a * extracted);
}
