#version 330

uniform sampler2D InSampler;
uniform sampler2D BloomSampler;

layout(std140) uniform SamplerInfo {
	vec2 OutSize;
	vec2 InSize;
};

in vec2 texCoord;

out vec4 fragColor;

void main() {
	vec4 base = texture(InSampler, texCoord);
	vec4 bloom = texture(BloomSampler, texCoord);
	fragColor = vec4(base.rgb + bloom.rgb, base.a);
}
