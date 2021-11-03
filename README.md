# Skija Example
JetBrains에서 공개한 Skija를 사용해서 이미지 처리 해보기 (리사이징, Gif 프레임 추출 등...)

## Environment
* Java 11
* Skija 0.93.4 (Linux인 경우 0.93.1)
* Spring Boot
* Spring WebFlux

## TODO
* DirectBuffer 사용하면서 메모리 터질 위험이 있는데 관련된 내용 공부해서 적용 필요
  * 일단 buffer clear 처리 해주니까 고해상도 이미지 요청 1000건 순식간에 때려도 안터졌는데, Buffer Pool이라던가 그외 배워야할 것들 참고할 필요 있음.

## 참고 링크
* [Skija](https://github.com/JetBrains/skija)