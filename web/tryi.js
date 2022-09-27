import { Base64Binary } from './base64.js'

export function render(src, canvas) {
    if (src === undefined || src === '') {
        return
    }

    const arr = src.split(';');

    const x = arr.length === 3 ? arr[0] : 255
    const y = arr.length === 3 ? arr[1] : 255
    const tryi =  arr.length === 3 ? arr[2] : src

    if (!isNumeric(x) && !isNumeric(y)) {
        console.error('height and width must be numbers');
        return
    }
    canvas.width = x;
    canvas.height = y;

    if (canvas.getContext) {
        const ctx = canvas.getContext('2d');

        // background
        ctx.fillStyle = `rgb(255, 255, 255)`;
        ctx.fillRect(0, 0, x, y);

        const xScale = x / 255
        const yScale = y / 255

        // get the triangles
        const intArray = Base64Binary.decode(tryi)
        // format is x1, y1, x2, y2, x3, y3, r, g, b, a
        for (let i = 0; i < intArray.length; i += 10) {
            ctx.beginPath();
            ctx.fillStyle = `rgba(${intArray[6+i]}, ${intArray[7+i]}, ${intArray[8+i]}, ${intArray[9+i] / 255})`;
            ctx.moveTo(intArray[i] * xScale, intArray[1+i] * yScale);
            ctx.lineTo(intArray[2+i] * xScale, intArray[3+i] * yScale);
            ctx.lineTo(intArray[4+i] * xScale, intArray[5+i] * yScale);
            ctx.fill();
        }
    }
}

function isNumeric(val) {
    return /^-?\d+$/.test(val);
}
