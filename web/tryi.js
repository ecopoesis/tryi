import { Base64Binary } from './base64.js'

export function render(tryi, x, y, canvas) {
    if (tryi === undefined || x === undefined || y === undefined || tryi === '' || x === '' || y === '') {
        return
    }
    if (!isNumeric(x) && !isNumeric(y)) {
        console.error('height and width must be numbers');
        return
    }
    canvas.width = x;
    canvas.height = y;

    if (canvas.getContext) {
        const ctx = canvas.getContext('2d');

        // background
        ctx.fillRect(0, 0, x, y);

        const xScale = x / 255
        const yScale = y / 255

        // get the triangles
        const intArray = Base64Binary.decode(tryi)
        // format is x1, y1, x2, y2, x3, y3, r, g, b, a
        for (let i = 0; i < intArray.length; i += 10) {
            ctx.beginPath();
            ctx.fillStyle = `rgba(${intArray[6+i]}, ${intArray[7+i]}, ${intArray[8+i]}, ${intArray[9+i] / 255})`;
            ctx.moveTo(intArray[0] * xScale, intArray[1+i] * yScale);
            ctx.lineTo(intArray[2+i] * xScale, intArray[3+i] * yScale);
            ctx.lineTo(intArray[4+i] * xScale, intArray[5+i] * yScale);
            ctx.fill();
        }
    }
}

function isNumeric(val) {
    return /^-?\d+$/.test(val);
}
