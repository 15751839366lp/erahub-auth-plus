<template>
  <div>
    <svg-icon icon-class="github" @click="goto" />
  </div>
</template>

<script setup>
const url = ref('https://gitee.com/JavaLionLi/erahub-Cloud-Plus');

function goto() {
  window.open(url.value)
}
</script>
