export function initScroll(plansScroll) {
  if (!plansScroll) return;

  let isDown = false;
  let startX;
  let scrollLeft;

  plansScroll.addEventListener('mousedown', (e) => {
    isDown = true;
    plansScroll.style.cursor = 'grabbing';
    startX = e.pageX - plansScroll.offsetLeft;
    scrollLeft = plansScroll.scrollLeft;
  });

  plansScroll.addEventListener('mouseleave', () => {
    isDown = false;
    plansScroll.style.cursor = 'grab';
  });

  plansScroll.addEventListener('mouseup', () => {
    isDown = false;
    plansScroll.style.cursor = 'grab';
  });

  plansScroll.addEventListener('mousemove', (e) => {
    if (!isDown) return;
    e.preventDefault();
    const x = e.pageX - plansScroll.offsetLeft;
    const walk = (x - startX) * 2;
    plansScroll.scrollLeft = scrollLeft - walk;
  });
}